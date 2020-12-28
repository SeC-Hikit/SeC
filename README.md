# SeC
SeC is a public and open API that manages high level geo-data related to trails.
It supports accessibility-notifications, maintenance, a variety of POIs and more features all linked with trails.
## Build
In order to build, run `mvn install` on the project root.
Requirements:
- JDK 8
- Maven 3
### Run
In order to run the application, you shall make a MongoDB instance available (for example, via [docker](https://hub.docker.com/_/mongo)).
Once you did so, configure the properties file:
```
# Web settings
server.servlet.contextPath=/api/v1/
server.port=8990
service.altitude.port=8080

# DB settings
db.name=your_db_name
db.uri=mongodb://localhost:30000

# Trail GPX Storage
trail.storage.path=your/trail/to/server
```
### UI
[An user interface](https://github.com/loreV/SeC-Frontend) has been developed to visualize and simplify working with trail data.

## Vision and challenges
S&C aims to be a user focused API, that easily allow to gather and save rich data about trails, while connecting this data to a community accessibility notifications, planned maintenance data and POI.
It aims to provide a first point of access for trails management while integrating to central storage via OSM by REST APIs.