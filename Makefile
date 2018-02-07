# because sbt. See: https://github.com/sbt/sbt/issues/3266

.PHONY: spark
spark: spark-2.0.2 spark-2.1.2 spark-2.2.0

.PHONY: spark-%
spark-%:
	sbt -DsparkVersion=$* -Dsbt.override.build.repos=true -Dsbt.repository.config=project/repositories docker

test: test-2.0.2 test-2.1.2

test-%:
	sbt -DsparkVersion=$* test

clean:
	sbt clean
