import * as cdk from 'aws-cdk-lib';
import { Construct } from 'constructs';
import * as apigateway from 'aws-cdk-lib/aws-apigateway'
import * as events from 'aws-cdk-lib/aws-events'
import * as lambda from 'aws-cdk-lib/aws-lambda'
import * as dynamodb from 'aws-cdk-lib/aws-dynamodb';
import * as sqs from 'aws-cdk-lib/aws-sqs';
import * as targets from 'aws-cdk-lib/aws-events-targets';
import * as lambdaEventSources from 'aws-cdk-lib/aws-lambda-event-sources';

export class InfrastructureStack extends cdk.Stack {
  constructor(scope: Construct, id: string, props?: cdk.StackProps) {
    super(scope, id, props);

    // The code that defines your stack goes here
    const DRIVER_TABLE_NAME = 'cloud-native-driver-mgmt';
    const DRIVER_TIPS_TABLE_NAME = 'cloud-native-driver-tips';

    const driversTable = new dynamodb.Table(this, 'DriversTable', {
      tableName: DRIVER_TABLE_NAME,
      billingMode: dynamodb.BillingMode.PAY_PER_REQUEST,
      partitionKey: {
        name: 'id',
        type: dynamodb.AttributeType.STRING
      },
      removalPolicy: cdk.RemovalPolicy.DESTROY
    });

    const driverTipsTable = new dynamodb.Table(this, 'DriverTipsTable', {
      tableName: DRIVER_TIPS_TABLE_NAME,
      billingMode: dynamodb.BillingMode.PAY_PER_REQUEST,
      partitionKey: {
        name: 'id',
        type: dynamodb.AttributeType.STRING
      },
      sortKey: {
        name: 'eventTime',
        type: dynamodb.AttributeType.STRING
      },
      removalPolicy: cdk.RemovalPolicy.DESTROY
    });
    const gsiProps: dynamodb.GlobalSecondaryIndexProps = {
        indexName: 'driverId_index',
        partitionKey: {
            name: 'driverId',
            type: dynamodb.AttributeType.STRING,
        }
    };
    driverTipsTable.addGlobalSecondaryIndex(gsiProps)

    const driverTipsEventQueue = new sqs.Queue(this, 'DriverTipsEventQueue', {
      queueName: 'driver-tips-event-queue'
    });

    const createDriver = new lambda.Function(this, 'CreateDriverLambda', {
      timeout: cdk.Duration.seconds(30),
      memorySize: 1024,
      functionName: 'createDriver',
      runtime: lambda.Runtime.JAVA_17,
      handler: 'io.moia.challenge.driver.DriverCreateHandler',
      code: lambda.Code.fromAsset('../build/libs/coding-challenge-cloud-native-driver-management-all.jar'),
      environment: {
        'DRIVER_TABLE_NAME': DRIVER_TABLE_NAME
      }
    });
    driversTable.grantFullAccess(createDriver)

    const getDriver = new lambda.Function(this, 'GetDriverLambda', {
      timeout: cdk.Duration.seconds(30),
      memorySize: 1024,
      functionName: 'getDriver',
      runtime: lambda.Runtime.JAVA_17,
      handler: 'io.moia.challenge.driver.DriverGetHandler',
      code: lambda.Code.fromAsset('../build/libs/coding-challenge-cloud-native-driver-management-all.jar'),
      environment: {
        'DRIVER_TABLE_NAME': DRIVER_TABLE_NAME
      }
    });
    driversTable.grantFullAccess(getDriver)


    // SQS Event (Tipping) Consumer
    const driverTipsConsumer = new lambda.Function(this, 'DriverTipsEventConsumerLambda', {
      timeout: cdk.Duration.seconds(30),
      memorySize: 1024,
      functionName: 'driverTipsConsumer',
      runtime: lambda.Runtime.JAVA_17,
      handler: 'io.moia.challenge.driver.DriverTipsConsumerHandler',
      code: lambda.Code.fromAsset('../build/libs/coding-challenge-cloud-native-driver-management-all.jar'),
      environment: {
        'DRIVER_TABLE_NAME': DRIVER_TIPS_TABLE_NAME
      }
    });
    driverTipsTable.grantFullAccess(driverTipsConsumer)
    driverTipsEventQueue.grantConsumeMessages(driverTipsConsumer)
    const eventSource = new lambdaEventSources.SqsEventSource(driverTipsEventQueue);
    driverTipsConsumer.addEventSource(eventSource);

    const getDriverTips = new lambda.Function(this, 'DriverGetTipsLambda', {
      timeout: cdk.Duration.seconds(30),
      memorySize: 1024,
      functionName: 'getDriverTips',
      runtime: lambda.Runtime.JAVA_17,
      handler: 'io.moia.challenge.driver.DriverGetTipsHandler',
      code: lambda.Code.fromAsset('../build/libs/coding-challenge-cloud-native-driver-management-all.jar'),
      environment: {
        'DRIVER_TABLE_NAME': DRIVER_TABLE_NAME,
        'DRIVER_TIPS_TABLE_NAME': DRIVER_TIPS_TABLE_NAME
      }
    });
    driversTable.grantReadData(getDriverTips)
    driverTipsTable.grantReadData(getDriverTips)

    // Test data generation Lambdas
    const driverTestDataHandler = new lambda.Function(this, 'DriverTestDataLambda', {
      timeout: cdk.Duration.seconds(30),
      memorySize: 1024,
      functionName: 'driverTestDataHandler',
      runtime: lambda.Runtime.JAVA_17,
      handler: 'io.moia.challenge.driver.testdata.CreateDriverTestDataHandler::handle',
      code: lambda.Code.fromAsset('../build/libs/coding-challenge-cloud-native-driver-management-all.jar'),
    });
    driversTable.grantFullAccess(driverTestDataHandler);
    const driverTestDataSchedule = new events.Rule(this, 'DriverTestDataSchedule', {
      schedule: events.Schedule.rate(cdk.Duration.minutes(1))
    })
    driverTestDataSchedule.addTarget(new targets.LambdaFunction(driverTestDataHandler))

    const driverTippingEventSampler = new lambda.Function(this, 'DriverTippingEventSampler', {
      timeout: cdk.Duration.seconds(30),
      memorySize: 1024,
      functionName: 'driverTippingEventSampler',
      runtime: lambda.Runtime.JAVA_17,
      handler: 'io.moia.challenge.driver.testdata.DriverTippingEventSampler::handle',
      code: lambda.Code.fromAsset('../build/libs/coding-challenge-cloud-native-driver-management-all.jar'),
      environment: {
        'DRIVER_TIPS_QUEUE_URL': driverTipsEventQueue.queueUrl,
        'TABLE_NAME': DRIVER_TABLE_NAME
      }
    });
    driverTipsEventQueue.grantSendMessages(driverTippingEventSampler)
    driversTable.grantFullAccess(driverTippingEventSampler)
    const driverTippingEventSchedule = new events.Rule(this, 'DriverTippingEventSchedule', {
      schedule: events.Schedule.expression('cron(*/2 * * * ? *)')
    })
    driverTippingEventSchedule.addTarget(new targets.LambdaFunction(driverTippingEventSampler))

    // API Gateway
    const api = new apigateway.RestApi(this, "driver-tips-api", {
      restApiName: "Driver Tips Service",
      description: "This service serves driver tips."
    });

    const apiDrivers = api.root.addResource('drivers')

    apiDrivers.addMethod('POST', new apigateway.LambdaIntegration(createDriver));

    const apiDriversId = apiDrivers.addResource('{id}')
    apiDriversId.addMethod('GET',  new apigateway.LambdaIntegration(getDriver));

    const driverTips = apiDriversId.addResource('tips').addResource('{period}')
    driverTips.addMethod('GET',  new apigateway.LambdaIntegration(getDriverTips));
  }
}
