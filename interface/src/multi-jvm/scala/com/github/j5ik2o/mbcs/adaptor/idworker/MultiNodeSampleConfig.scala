package com.github.j5ik2o.mbcs.adaptor.idworker

import akka.remote.testkit.MultiNodeConfig
import com.typesafe.config.ConfigFactory

object MultiNodeSampleConfig extends MultiNodeConfig {
  val node1      = role("node1")
  val node2      = role("node2")
  val node3      = role("node3")

  commonConfig(
    ConfigFactory.parseString(
      """
        |akka {
        |  stdout-loglevel = off // defaults to WARNING can be disabled with off. The stdout-loglevel is only in effect during system startup and shutdown
        |  log-dead-letters-during-shutdown = on
        |  loglevel = debug
        |  log-dead-letters = on
        |  log-config-on-start = off // Log the complete configuration at INFO level when the actor system is started
        |
        |  loggers = ["akka.event.slf4j.Slf4jLogger"]
        |  logging-filter = "akka.event.slf4j.Slf4jLoggingFilter"
        |
        |  actor {
        |    debug {
        |      receive = on // log all messages sent to an actor if that actors receive method is a LoggingReceive
        |      autoreceive = off // log all special messages like Kill, PoisoffPill etc sent to all actors
        |      lifecycle = off // log all actor lifecycle events of all actors
        |      fsm = off // enable logging of all events, transitioffs and timers of FSM Actors that extend LoggingFSM
        |      event-stream = off // enable logging of subscriptions (subscribe/unsubscribe) on the ActorSystem.eventStream
        |    }
        |  }
        |}
        |
        |akka.actor.provider = "akka.cluster.ClusterActorRefProvider"
        |
        |akka.persistence.journal.plugin = "dynamo-db-journal"
        |akka.persistence.snapshot-store.plugin = "dynamo-db-snapshot"
        |
        |dynamo-db-journal {
        |  class = "com.github.j5ik2o.akka.persistence.dynamodb.journal.DynamoDBJournal"
        |  plugin-dispatcher = "akka.actor.default-dispatcher"
        |  dynamodb-client {
        |    access-key-id = "x"
        |    secret-access-key = "x"
        |    endpoint = "http://127.0.0.1:8000/"
        |  }
        |}
        |
        |dynamo-db-snapshot {
        |  class = "com.github.j5ik2o.akka.persistence.dynamodb.snapshot.DynamoDBSnapshotStore"
        |  plugin-dispatcher = "akka.actor.default-dispatcher"
        |  dynamodb-client {
        |    access-key-id = "x"
        |    secret-access-key = "x"
        |    endpoint = "http://127.0.0.1:8000/"
        |  }
        |}
        |
        |dynamo-db-read-journal {
        |  class = "com.github.j5ik2o.akka.persistence.dynamodb.query.DynamoDBReadJournalProvider"
        |  write-plugin = "dynamo-db-journal"
        |  dynamodb-client {
        |    access-key-id = "x"
        |    secret-access-key = "x"
        |    endpoint = "http://127.0.0.1:8000/"
        |  }
        |}
        |
        |akka.cluster.metrics.enabled=off
      """.stripMargin
    )
  )

}
