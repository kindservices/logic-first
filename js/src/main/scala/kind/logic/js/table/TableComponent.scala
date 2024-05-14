package kind.logic.js.table

import zio.*

class TableComponent(identifier: String, dbRef: Ref[Map[String, Json]], columns: Seq[String] = Nil)

object TableComponent {

  def apply(dbRef: Ref[Map[String, Json]]) = {
    dbRef.get.map { db =>
      db
      val columns = Nil
      new TableComponent("", dbRef, columns)
    }
  }
}
