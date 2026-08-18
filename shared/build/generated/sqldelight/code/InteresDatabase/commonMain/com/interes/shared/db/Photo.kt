package com.interes.shared.db

import kotlin.Long
import kotlin.String

public data class Photo(
  public val id: Long,
  public val boardId: Long,
  public val filePath: String,
  public val orderIndex: Long,
  public val width: Long,
  public val height: Long,
  public val addedAt: Long,
)
