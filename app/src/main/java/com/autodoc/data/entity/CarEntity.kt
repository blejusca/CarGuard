package com.autodoc.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "cars")
data class CarEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,

    val brand: String,
    val model: String,
    val plate: String,
    val year: Int,
    val engine: String,

    val ownerName: String = "",
    val ownerPhone: String = "",
    val ownerEmail: String = "",
    val ownerNotes: String = ""
)