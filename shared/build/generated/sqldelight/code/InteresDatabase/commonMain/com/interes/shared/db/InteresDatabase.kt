package com.interes.shared.db

import app.cash.sqldelight.Transacter
import app.cash.sqldelight.db.QueryResult
import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.db.SqlSchema
import com.interes.shared.db.shared.newInstance
import com.interes.shared.db.shared.schema
import kotlin.Unit

public interface InteresDatabase : Transacter {
  public val boardQueries: BoardQueries

  public val photoQueries: PhotoQueries

  public companion object {
    public val Schema: SqlSchema<QueryResult.Value<Unit>>
      get() = InteresDatabase::class.schema

    public operator fun invoke(driver: SqlDriver): InteresDatabase = InteresDatabase::class.newInstance(driver)
  }
}
