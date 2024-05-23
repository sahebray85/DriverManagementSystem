# Coding Challenge

Driver Management Service

## Introduction

This repository contains a service managing drivers. 
The initial version contains REST endpoints to get and create drivers.

The service is implemented in `Kotlin` running on `AWS Lambda`.
[CDK](https://aws.amazon.com/cdk/) is used to deploy it.

The service uses a `DynamoDb` table to store the drivers.

## Deploy and run

First make sure that CDK is installed - see the [CDK documentation](https://docs.aws.amazon.com/cdk/v2/guide/getting_started.html#getting_started_install) for details.
```
npm install -g aws-cdk
```

Afterwards you can deploy the service. 
```
./gradlew shadowJar
cd infrastructure && npx cdk deploy
```

Now you should be able to interact with the REST API of the service.
The API endpoints can be found in the output that `cdk deploy` produces.

```
curl -X POST https://<your-api-id>.execute-api.eu-central-1.amazonaws.com/dev/drivers -d '{"firstname": "Some", "lastname": "One", "driverLicenseId": "4711"}'

curl  https://<your-api-id>.execute-api.eu-central-1.amazonaws.com/dev/drivers/ff79773c-0bf7-46ac-aeda-441f700580d4
```

## Task Specification

We want to add the functionality of **driver tipping** to our system. 
Other services have already implemented the feature of accepting tips from a customer and also paying out tips to the driver. 

Our driver service **should now also keep track of the tips a driver received**.
It should be extended to be able to **aggregate the tips** received **by each driver on a weekly and daily level**.

The tipping events are available via the [SQS](https://aws.amazon.com/de/sqs/) queue `driver-tips-event-queue`.
These events contain information about the driver receiving the tip and the amount tipped.

```
{
    "driverId": "275d7bb8-3a2f-432c-8435-5a01c64ca6ba",
    "amount": "7.33"
    "eventTime": "2019-09-16T10:58:14.651Z"
}
```

The `io.moia.challenge.driver.testdata.DriverTippingEventSampler` runs every minute to simulate incoming tipping events.

**Your task is to:** 
- consume tipping events and store the information in a suitable way. It would be great if you could explain shortly why you decided for your strategy to store the tips (and which alternatives you discarded).
- extend the REST API of the service by an endpoint which exposes the aggregated amount of tips received by a specific driver today and in the current week.
- improve the quality of the existing code and adapt it to your needs. The existing project and code definitely has a few flaws. **The complete code you hand in should meet your own quality standards.**

**Please also provide a few sentences regarding the decisions you made and the reasoning behind them.**
It might happen that you cannot reach the solution you envision e.g. because of time constraints or lack of technology skills. This is fine - please leave a few sentences explaining the envisioned solution.

If you like to deploy and run the code in a real environment, please consider creating a [AWS Free Tier Account](https://aws.amazon.com/free).

## Clean up

To remove the traces of this application on your AWS account make sure you clean up after you finished:

```
cd infrastructure && npx cdk destroy
```
---
## Solution

### Consume Tipping Amount
A Lambda is added to consume events from SQS queue and write (put_item) to dynamo DB. The Lambda subscribes to SQS event stream and processes the event data,
it adds tipping id as UUID and creation date (current datetime) while persisting to DynamoDB

#### Other Discarded Alternatives
1. AWS ElastiCache - Tipping amount can be stored in elasticache, which high availability and low latency throughput. It would be suitable to aggregate the tipping amount for a given driver id and time period. However, the data can't be persisted for longer duration; so, we can't perform analytics on tipping amount. Thus the approach is discarded.                     
2. AWS RDS - We can choose AWS RDS to store tipping amount, RDS guarantees data consistency which comes with a costs of availability, we don’t require it. It is not scalable compared to DynamoDB , as storing tipping amount is write heavy system. Moreover, data aggregation is much slower. Thus, this approach is also discarded. 

### Aggregate Tipping Amount
An API Gateway is exposed to fetch the aggregation amount for a given driver_id and time period. The API invokes a serverless lambda which fetches tipping data from DynamoDB and sends back aggregated response.
```
API Signature:
Method: GET
URL: https://{{api_id}}.execute-api.us-east-1.amazonaws.com/prod/drivers/{{driver_id}}/tips/{{period}}
```

```
api_id: API GateWay ID
driver_id: Driver UUID (from Driver management Database.) e.g: `337bc711-4645-483b-a7dd-27a55753db79`
period: Time period of the aggregation. Supported Values 
        today: Get Aggregated tipping amount for today
        week: Get  Aggregated tipping amount for last 7 days (a relative week)
```
    