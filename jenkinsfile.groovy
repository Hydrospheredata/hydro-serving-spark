def repository = 'hydro-serving-spark'

def versions = [
        "2.0",
        "2.1",
        "2.2"
]

def sparkImages = versions.collect {"hydrosphere/serving-runtime-spark-${it}"}

def buildFunction = {
    sh "make test"
    sh "make"
}

def collectTestResults = {
    junit testResults: '**/target/test-reports/io.hydrosphere*.xml', allowEmptyResults: true
}

pipelineCommon(
        repository,
        false, //needSonarQualityGate,
        sparkImages,
        collectTestResults,
        buildFunction,
        buildFunction,
        buildFunction
)