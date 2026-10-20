package com.example.data

import android.content.Context
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverter
import androidx.room.TypeConverters
import androidx.room.Update
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.flow.Flow

class Converters {
    private val moshi = Moshi.Builder().add(KotlinJsonAdapterFactory()).build()

    @TypeConverter
    fun fromStatusChangeList(list: List<StatusChange>?): String {
        if (list == null) return "[]"
        val type = Types.newParameterizedType(List::class.java, StatusChange::class.java)
        return moshi.adapter<List<StatusChange>>(type).toJson(list)
    }

    @TypeConverter
    fun toStatusChangeList(json: String?): List<StatusChange> {
        if (json.isNullOrEmpty()) return emptyList()
        val type = Types.newParameterizedType(List::class.java, StatusChange::class.java)
        return moshi.adapter<List<StatusChange>>(type).fromJson(json) ?: emptyList()
    }

    @TypeConverter
    fun fromBillItemList(list: List<BillItem>?): String {
        if (list == null) return "[]"
        val type = Types.newParameterizedType(List::class.java, BillItem::class.java)
        return moshi.adapter<List<BillItem>>(type).toJson(list)
    }

    @TypeConverter
    fun toBillItemList(json: String?): List<BillItem> {
        if (json.isNullOrEmpty()) return emptyList()
        val type = Types.newParameterizedType(List::class.java, BillItem::class.java)
        return moshi.adapter<List<BillItem>>(type).fromJson(json) ?: emptyList()
    }

    @TypeConverter
    fun fromPriority(value: Priority): String = value.name

    @TypeConverter
    fun toPriority(value: String): Priority = try { Priority.valueOf(value) } catch (e: Exception) { Priority.MEDIUM }

    @TypeConverter
    fun fromServiceStatus(value: ServiceStatus): String = value.name

    @TypeConverter
    fun toServiceStatus(value: String): ServiceStatus = try { ServiceStatus.valueOf(value) } catch (e: Exception) { ServiceStatus.CREATED }

    @TypeConverter
    fun fromWorkerStatus(value: WorkerStatus): String = value.name

    @TypeConverter
    fun toWorkerStatus(value: String): WorkerStatus = try { WorkerStatus.valueOf(value) } catch (e: Exception) { WorkerStatus.IDLE }
}

@Dao
interface ReLectDao {
    // Customers
    @Query("SELECT * FROM customers")
    fun getCustomersFlow(): Flow<List<Customer>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCustomer(customer: Customer)

    @Update
    suspend fun updateCustomer(customer: Customer)

    @Query("DELETE FROM customers WHERE id = :id")
    suspend fun deleteCustomer(id: String)

    // Services
    @Query("SELECT * FROM services")
    fun getServicesFlow(): Flow<List<ServiceRequest>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertServiceRequest(request: ServiceRequest)

    @Update
    suspend fun updateServiceRequest(request: ServiceRequest)

    @Query("DELETE FROM services WHERE id = :id")
    suspend fun deleteServiceRequest(id: String)

    // Workers
    @Query("SELECT * FROM workers")
    fun getWorkersFlow(): Flow<List<Worker>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertWorker(worker: Worker)

    @Update
    suspend fun updateWorker(worker: Worker)

    @Query("DELETE FROM workers WHERE id = :id")
    suspend fun deleteWorker(id: String)

    // Inventory
    @Query("SELECT * FROM inventory")
    fun getInventoryFlow(): Flow<List<InventoryItem>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertInventoryItem(item: InventoryItem)

    @Update
    suspend fun updateInventoryItem(item: InventoryItem)

    @Query("DELETE FROM inventory WHERE id = :id")
    suspend fun deleteInventoryItem(id: String)

    // Purchases
    @Query("SELECT * FROM purchases")
    fun getPurchasesFlow(): Flow<List<PurchaseRecord>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPurchaseRecord(record: PurchaseRecord)

    // Bills
    @Query("SELECT * FROM bills")
    fun getBillsFlow(): Flow<List<Bill>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBill(bill: Bill)

    @Update
    suspend fun updateBill(bill: Bill)

    @Query("DELETE FROM bills WHERE id = :id")
    suspend fun deleteBill(id: String)

    // Expenses
    @Query("SELECT * FROM expenses")
    fun getExpensesFlow(): Flow<List<Expense>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertExpense(expense: Expense)

    @Query("DELETE FROM expenses WHERE id = :id")
    suspend fun deleteExpense(id: String)

    // Attendance
    @Query("SELECT * FROM attendance")
    fun getAttendanceFlow(): Flow<List<AttendanceRecord>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAttendanceRecord(record: AttendanceRecord)

    @Update
    suspend fun updateAttendanceRecord(record: AttendanceRecord)

    // Users Authentication & RBAC
    @Query("SELECT * FROM users")
    fun getUsersFlow(): Flow<List<UserEntity>>

    @Query("SELECT * FROM users")
    suspend fun getAllUsersList(): List<UserEntity>

    @Query("SELECT * FROM users WHERE LOWER(email) = LOWER(:email) LIMIT 1")
    suspend fun getUserByEmail(email: String): UserEntity?

    @Query("SELECT * FROM users WHERE phone = :phone LIMIT 1")
    suspend fun getUserByPhone(phone: String): UserEntity?

    @Query("SELECT * FROM users WHERE id = :id LIMIT 1")
    suspend fun getUserById(id: String): UserEntity?

    @Query("SELECT COUNT(*) FROM users")
    suspend fun getUserCount(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUser(user: UserEntity)

    @Update
    suspend fun updateUser(user: UserEntity)

    @Query("DELETE FROM users WHERE id = :id")
    suspend fun deleteUser(id: String)

    // OTP Records
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOtp(record: OtpRecord)

    @Update
    suspend fun updateOtp(record: OtpRecord)

    @Query("SELECT * FROM otp_records WHERE target = :target AND purpose = :purpose AND isUsed = 0 AND expiresAt > :currentTime ORDER BY createdAt DESC LIMIT 1")
    suspend fun getActiveOtp(target: String, purpose: String, currentTime: Long): OtpRecord?

    @Query("UPDATE otp_records SET isUsed = 1 WHERE target = :target")
    suspend fun invalidateOtpsForTarget(target: String)

    // Audit Logs
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAuditLog(log: AuditLogEntry)

    @Query("SELECT * FROM audit_logs ORDER BY timestamp DESC LIMIT 200")
    fun getAuditLogsFlow(): Flow<List<AuditLogEntry>>

    // Clear Tables for "Clear Toy Data"
    @Query("DELETE FROM customers")
    suspend fun clearCustomers()

    @Query("DELETE FROM services")
    suspend fun clearServiceRequests()

    @Query("DELETE FROM workers")
    suspend fun clearWorkers()

    @Query("DELETE FROM inventory")
    suspend fun clearInventory()

    @Query("DELETE FROM purchases")
    suspend fun clearPurchases()

    @Query("DELETE FROM bills")
    suspend fun clearBills()

    @Query("DELETE FROM expenses")
    suspend fun clearExpenses()

    @Query("DELETE FROM attendance")
    suspend fun clearAttendance()
}

@Database(
    entities = [
        Customer::class,
        ServiceRequest::class,
        Worker::class,
        InventoryItem::class,
        PurchaseRecord::class,
        Bill::class,
        Expense::class,
        AttendanceRecord::class,
        UserEntity::class,
        OtpRecord::class,
        AuditLogEntry::class
    ],
    version = 5,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun reLectDao(): ReLectDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "relect_database"
                )
                .fallbackToDestructiveMigration(dropAllTables = true)
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
