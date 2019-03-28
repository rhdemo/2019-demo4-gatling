# Running load test locally

The load test runs from within a container so you need to ensure the
brokers argument passed here is reachable from within a container.

```shell
JAVA_OPTS="-Dbrokers=<ip address>:9092 -Dtopic=test-topic-1-1 -Dusers=5 -Dseconds=10" ./run-gatling.sh
```

# Building the load test image

```shell
./build.sh
```

## Publishing to quay.io

```shell
docker push quay.io/bbrowning/demo2019-hard-shake-load-test
```

# Running in OpenShift

```shell
oc run load-test -it --rm=true --restart=Never --image=quay.io/bbrowning/demo2019-hard-shake-load-test --image-pull-policy=Always --env="JAVA_OPTS=-Dbrokers=scale-test-kafka-bootstrap.kafka-knative-scale-tests.svc.cluster.local:9092 -Dtopic=test-topic-1-1 -Dusers=5 -Dseconds=10"
```
