package com.team6.travel_app.data

import android.database.Cursor
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.RawQuery
import androidx.sqlite.db.SupportSQLiteQuery

@Dao
interface CartDao {
    @Insert
    suspend fun insertEntity(cart: Cart)
    @Insert
    fun insertAll(vararg cart: Cart): List<Long>
    @RawQuery
    fun getEntities(query : SupportSQLiteQuery) : List<Long>
    @Query("SELECT * FROM cart")
            /* var liveUsers: LiveData<MutableList<User?>?>? =
                 rawDao.getUsers(SimpleSQLiteQuery("SELECT * FROM User ORDER BY name DESC"))*/
    fun getAllEntities() : List<Cart>
    @Query("SELECT * FROM CART WHERE id=:id")
    fun getById(id: Int) : Cart
    @Query(value = "DELETE FROM cart")
    suspend fun deleteAllRecords()
    @Query("DELETE FROM cart WHERE id =  :id")
    suspend fun delete(id: Int) : Int
    @Query("SELECT id FROM cart WHERE id =  :id")
    fun searchForEntity(id: Int): Int
    @Query("UPDATE cart SET isAddedToCart = 0 WHERE id = :id")
    fun updateEntity(id:Int)
    @Query("SELECT COUNT(*) FROM cart")
    fun rowCount() : Int
    @Query("UPDATE cart SET quantity = :quantity WHERE id = :id")
    fun updateQuantity(id: Int,quantity:Int)
    @Query("SELECT quantity FROM cart WHERE id = :id")
    fun getQuantity(id: Int?) : Int
    @Query("SELECT price FROM cart WHERE id = :id")
    fun getPrice(id:Int) : Int
    @Query("SELECT * FROM cart")
    fun getCursorAll(): Cursor?
    @Query("SELECT CASE WHEN isAddedToCart = 1 THEN 'true' ELSE 'false' END AS isAddedToCart FROM cart WHERE id = :id")
    fun isAddedToCart(id : Int) : Boolean

    @Query("SELECT id FROM cart WHERE id = :id")
    suspend fun searchEntity(id : Int) : Int?

    @Query("Update cart set isDeposited = :isDeposited where id = :id")
    fun setIsDeposited(id: Int,isDeposited : Int)

}