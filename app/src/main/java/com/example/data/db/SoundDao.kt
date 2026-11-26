package com.example.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface SoundDao {
    @Query("SELECT * FROM sound_cards ORDER BY isFavorite DESC, sortOrder ASC, createdAt DESC")
    fun observeAllCards(): Flow<List<SoundCardEntity>>

    @Query("SELECT * FROM sound_cards ORDER BY isFavorite DESC, sortOrder ASC, createdAt DESC")
    suspend fun getAllCardsOnce(): List<SoundCardEntity>

    @Query("SELECT * FROM sound_cards WHERE id = :id LIMIT 1")
    suspend fun getCardById(id: Long): SoundCardEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCard(card: SoundCardEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCards(cards: List<SoundCardEntity>)

    @Update
    suspend fun updateCard(card: SoundCardEntity)

    @Query("UPDATE sound_cards SET title = :newTitle WHERE id = :id")
    suspend fun renameCard(id: Long, newTitle: String)

    @Query("UPDATE sound_cards SET isFavorite = :favorite WHERE id = :id")
    suspend fun setFavorite(id: Long, favorite: Boolean)

    @Query("DELETE FROM sound_cards WHERE id = :id")
    suspend fun deleteCardById(id: Long)
}
