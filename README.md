# Load tests

There are three load tests implemented currently -
`HardShakeSimulation`, `PredictionSimulation`, and
`E2ESimulation`. You can set the `SIMULATION` environment variable to
pick which one to run. It defaults to `HardShakeSimulation`.

Other configuration is specific to each load test and passed as system
properties in `JAVA_OPTS` like the examples below.

# Running a load test locally

The HardShakeSimulation load test runs from within a container so you
need to ensure the brokers argument passed here is reachable from
within a container.

```shell
SIMULATION="HardShakeSimulation" JAVA_OPTS="-Dbrokers=<ip address>:9092 -Dtopic=test-topic-1-1 -Dusers=5 -Dseconds=10" ./run-gatling.sh
```

PredictionSimulation example:

```shell
SIMULATION="PredictionSimulation" JAVA_OPTS="-Dhost=tf-serving-knative-demo.tf-demo.example.com -Dgateway=<your istio-ingressgateway address> -Dusers=5 -Dseconds=10" ./run-gatling.sh
```

E2ESimulation example:

```shell
SIMULATION="E2ESimulation" JAVA_OPTS="-Dhost=LIVE -Dusers=100 -Ddances=10" ./run-gatling.sh
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

HardShakeSimulation example:
```shell
oc run load-test -it --rm=true --restart=Never --requests="cpu=2" --image=quay.io/bbrowning/demo2019-hard-shake-load-test --image-pull-policy=Always --env="JAVA_OPTS=-Dbrokers=scale-test-kafka-bootstrap.kafka-knative-scale-tests.svc.cluster.local:9092 -Dtopic=test-topic-1-1 -Dusers=5 -Dseconds=10"
```

PredictionSimulation example:
```shell
oc run load-test -it --rm=true --restart=Never --requests="cpu=2" --image=quay.io/bbrowning/demo2019-hard-shake-load-test --image-pull-policy=Always --env="SIMULATION=PredictionSimulation" --env="JAVA_OPTS=-Dhost=tf-serving-knative-demo.tf-demo.example.com -Dgateway=istio-ingressgateway.istio-system -Dusers=5 -Dseconds=10"
```

E2ESimulation example:
```shell
oc run load-test -it --rm=true --restart=Never --requests="cpu=2" --image=quay.io/bbrowning/demo2019-hard-shake-load-test --image-pull-policy=Always --env="SIMULATION=E2ESimulation" --env="JAVA_OPTS=-Dhost=LIVE -Dusers=50 -Ddances=15"

# If you want to see the Gatling report, in another terminal:
oc cp load-test:/results /tmp/e2eresults
firefox /tmp/e2eresults/*/index.html

# And then, in the original terminal, CTRL+C to kill the load-test pod
```
