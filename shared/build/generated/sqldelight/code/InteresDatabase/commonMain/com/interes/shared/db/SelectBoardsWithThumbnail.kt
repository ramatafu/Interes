package com.interes.shared.db

import kotlin.Long
import kotlin.String

public data class SelectBoardsWithThumbnail(
  public val id: Long,
  public val title: String,
  public val category: String,
  public val createdAt: Long,
  public val thumbnailPath: String?,
)
