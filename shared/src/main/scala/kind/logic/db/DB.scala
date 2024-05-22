package kind.logic.db

import zio.*

/** Represents a database store
  */
trait DB[K, V] {
  def save(data: V): Task[K]
}

object DB {

  case class InMemorySeq[A](ref: Ref[Seq[A]]) extends DB[Int, A] {
    override def save(data: A) = {
      ref.modify { db =>
        val updated = data +: db
        val id      = updated.size
        (id, updated)
      }
    }
  }

  def inMemory[A]: UIO[InMemorySeq[A]] = {
    for db <- Ref.make(Seq.empty[A])
    yield InMemorySeq(db)
  }

  case class InMemoryKeyValue[K, V](byIdRef: Ref[Map[K, V]], makeKey: (Map[K, V], V) => K)
      extends DB[K, V] {
    override def save(data: V): Task[K] = {
      byIdRef.modify { db =>
        val id    = makeKey(db, data)
        val newDb = db + (id -> data)
        (id, newDb)
      }
    }
  }

  def inMemoryKeyValue[V]: UIO[DB.InMemoryKeyValue[Long, V]] = {
    inMemoryKeyValue[Long, V]((db, _) => db.size + 1)
  }

  def inMemoryKeyValue[K, V](makeKey: (Map[K, V], V) => K): UIO[DB.InMemoryKeyValue[K, V]] = {
    for db <- Ref.make(Map.empty[K, V])
    yield InMemoryKeyValue(db, makeKey)
  }
}
