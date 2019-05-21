Recommendation API Example with Hazelcast Cloud
----------------------------------------------

This application is a good example for using Hazelcast Cloud.
It covers a simple product recommendation system,
which tracks all the products opened by users, counts how many times they get a visit
and gives recommendation based on these details.

Prerequisites
-------------
JDK 8 installed on machine

Usage
-----
- Clone the repository, go to root directory of your cloned repository
- Update `src/main/resources/application.properties` with your Hazelcast Cloud details (replace all `TODO`)
  ```properties
  hazelcast.clusterGroupName=TODO
  hazelcast.clusterGroupPassword=TODO
  hazelcast.discoveryToken=TODO
  ```
- Build and Start the application
  ```bash
  ./mvnw clean install package && java -jar target/recommendation-api-0.0.1-SNAPSHOT.jar
  ```

Endpoints
---------

`GET /product` - list all the available products

`GET /product/ID` - get one product details by it's ID

`GET /product/ID/recommendations` - get recommendations for the product with ID

`GET /product/recommendations` - get general recommendations, based on current user product visits
