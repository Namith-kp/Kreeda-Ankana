package com.kreedaankana.data.repository

import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.kreedaankana.data.model.Booking
import kotlinx.coroutines.tasks.await

class BookingRepository {

    private val db = FirebaseFirestore.getInstance()
    private val bookingsCollection = db.collection("bookings")

    suspend fun bookSlot(booking: Booking): Result<Booking> {
        return try {
            // Check for conflicts first
            val conflictResult = checkConflict(booking.date, booking.startTime, booking.endTime)
            if (conflictResult.isFailure) return Result.failure(conflictResult.exceptionOrNull()!!)

            val hasConflict = conflictResult.getOrDefault(false)
            if (hasConflict) {
                return Result.failure(Exception("Slot already booked! Please choose a different time."))
            }

            val docRef = bookingsCollection.document()
            val bookingWithId = booking.copy(bookingId = docRef.id)
            docRef.set(bookingWithId).await()
            Result.success(bookingWithId)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private suspend fun checkConflict(date: String, startTime: String, endTime: String): Result<Boolean> {
        return try {
            val snapshot = bookingsCollection
                .whereEqualTo("date", date)
                .get().await()

            val existingBookings = snapshot.toObjects(Booking::class.java)
            val hasConflict = existingBookings.any { existing ->
                timeOverlaps(startTime, endTime, existing.startTime, existing.endTime)
            }
            Result.success(hasConflict)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun timeOverlaps(newStart: String, newEnd: String, existStart: String, existEnd: String): Boolean {
        return try {
            val newS = parseTime(newStart)
            val newE = parseTime(newEnd)
            val exS = parseTime(existStart)
            val exE = parseTime(existEnd)
            newS < exE && newE > exS
        } catch (e: Exception) {
            false
        }
    }

    private fun parseTime(time: String): Int {
        val parts = time.split(":")
        return parts[0].toInt() * 60 + parts[1].toInt()
    }

    suspend fun getBookingsByDate(date: String): Result<List<Booking>> {
        return try {
            val snapshot = bookingsCollection
                .whereEqualTo("date", date)
                .get().await()
            val bookings = snapshot.toObjects(Booking::class.java)
            Result.success(bookings)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getAllBookings(): Result<List<Booking>> {
        return try {
            val snapshot = bookingsCollection
                .get().await()
            val bookings = snapshot.toObjects(Booking::class.java)
            Result.success(bookings)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getBookingsByTeam(teamId: String): Result<List<Booking>> {
        return try {
            val snapshot = bookingsCollection
                .whereEqualTo("teamId", teamId)
                .get().await()
            val bookings = snapshot.toObjects(Booking::class.java)
            Result.success(bookings)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun cancelBooking(bookingId: String): Result<Unit> {
        return try {
            bookingsCollection.document(bookingId).delete().await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
