import * as cdk from 'aws-cdk-lib';
import { Construct } from 'constructs';
import * as apigateway from 'aws-cdk-lib/aws-apigateway'
import * as events from 'aws-cdk-lib/aws-events'
import * as lambda from 'aws-cdk-lib/aws-lambda'
import * as dynamodb from 'aws-cdk-lib/aws-dynamodb';
import * as sqs from 'aws-cdk-lib/aws-sqs';
import * as targets from 'aws-cdk-lib/aws-events-targets';

export class InfrastructureStack extends cdk.Stack {
  constructor(scope: Construct, id: string, props?: cdk.StackProps) {
    super(scope, id, props);

    // The code that defines your stack goes here
    const DRIVER_TABLE_NAME = 'cloud-native-driver-mgmt';

    const driversTable = new dynamodb.Table(this, 'DriversTable', {
      tableName: DRIVER_TABLE_NAME,
      billingMode: dynamodb.BillingMode.PAY_PER_REQUEST,
      partitionKey: {
        name: 'id',
        type: dynamodb.AttributeType.STRING
      },
      removalPolicy: cdk.RemovalPolicy.DESTROY
    });

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
        'TABLE_NAME': DRIVER_TABLE_NAME
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
        'TABLE_NAME': DRIVER_TABLE_NAME
      }
    });
    driversTable.grantFullAccess(getDriver)

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
        'DRIVER_TIPS_QUEUE_URL': driverTipsEventQueue.queueUrl
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
  }
}
