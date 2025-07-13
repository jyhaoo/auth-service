# auth-service

### Things I've ran into

When first creating the project and trying to build in VS Code I ran into an error while trying to build
build.gradle was looking for an actual running db to test on
You need to change the runtime: 'org.postgresql:postgresql' => 'com.h2database:h2'
