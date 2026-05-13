package com.kreedaankana.data.repository

import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.kreedaankana.data.model.Tournament
import com.kreedaankana.data.model.TournamentMatch
import kotlinx.coroutines.tasks.await

class TournamentRepository {
    private val firestore = FirebaseFirestore.getInstance()
    private val tournamentsCollection = firestore.collection("tournaments")
    private val matchesCollection = firestore.collection("tournament_matches")

    suspend fun createTournament(tournament: Tournament): Result<Unit> {
        return try {
            val docRef = tournamentsCollection.document()
            val tournamentWithId = tournament.copy(id = docRef.id)
            docRef.set(tournamentWithId).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getTournaments(): Result<List<Tournament>> {
        return try {
            val snapshot = tournamentsCollection
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .get().await()
            Result.success(snapshot.toObjects(Tournament::class.java))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getTournamentMatches(tournamentId: String): Result<List<TournamentMatch>> {
        return try {
            val snapshot = matchesCollection
                .whereEqualTo("tournamentId", tournamentId)
                .get().await()
            val matches = snapshot.toObjects(TournamentMatch::class.java)
            // Sort by round hierarchy and matchOrder
            val sortedMatches = matches.sortedWith(compareBy(
                { getRoundWeight(it.roundName) },
                { it.matchOrder }
            ))
            Result.success(sortedMatches)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun getRoundWeight(roundName: String): Int {
        return when (roundName) {
            "Quarterfinals" -> 1
            "Semifinals" -> 2
            "Finals" -> 3
            else -> 4
        }
    }

    suspend fun registerTeamForTournament(tournamentId: String, teamId: String, teamName: String): Result<Unit> {
        return try {
            val docRef = tournamentsCollection.document(tournamentId)
            val snapshot = docRef.get().await()
            if (!snapshot.exists()) return Result.failure(Exception("Tournament not found"))

            val tournament = snapshot.toObject(Tournament::class.java) ?: return Result.failure(Exception("Parse error"))

            if (tournament.registeredTeamIds.contains(teamId)) {
                return Result.failure(Exception("Your team is already registered for this tournament!"))
            }

            if (tournament.registeredTeamIds.size >= tournament.maxTeams) {
                return Result.failure(Exception("Tournament is already full!"))
            }

            val updatedIds = tournament.registeredTeamIds + teamId
            val updatedNames = tournament.registeredTeamNames + teamName

            docRef.update(
                "registeredTeamIds", updatedIds,
                "registeredTeamNames", updatedNames
            ).await()

            // If tournament is now full, automatically generate bracket matches and activate tournament!
            if (updatedIds.size >= tournament.maxTeams) {
                val fullTournament = tournament.copy(
                    registeredTeamIds = updatedIds,
                    registeredTeamNames = updatedNames
                )
                generateBrackets(fullTournament)
            }

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private suspend fun generateBrackets(tournament: Tournament) {
        val batch = firestore.batch()
        val teams = tournament.registeredTeamNames.shuffled() // Randomized seeding!
        val tournamentId = tournament.id

        if (tournament.maxTeams == 4) {
            // Finals
            val finalMatchRef = matchesCollection.document()
            val finalMatch = TournamentMatch(
                id = finalMatchRef.id,
                tournamentId = tournamentId,
                roundName = "Finals",
                matchOrder = 1
            )
            batch.set(finalMatchRef, finalMatch)

            // Semifinal 1
            val semi1Ref = matchesCollection.document()
            val semi1 = TournamentMatch(
                id = semi1Ref.id,
                tournamentId = tournamentId,
                roundName = "Semifinals",
                team1Name = teams.getOrElse(0) { "TBD" },
                team2Name = teams.getOrElse(1) { "TBD" },
                nextMatchId = finalMatchRef.id,
                matchOrder = 1
            )
            batch.set(semi1Ref, semi1)

            // Semifinal 2
            val semi2Ref = matchesCollection.document()
            val semi2 = TournamentMatch(
                id = semi2Ref.id,
                tournamentId = tournamentId,
                roundName = "Semifinals",
                team1Name = teams.getOrElse(2) { "TBD" },
                team2Name = teams.getOrElse(3) { "TBD" },
                nextMatchId = finalMatchRef.id,
                matchOrder = 2
            )
            batch.set(semi2Ref, semi2)

        } else { // 8 Teams
            // Finals
            val finalMatchRef = matchesCollection.document()
            val finalMatch = TournamentMatch(
                id = finalMatchRef.id,
                tournamentId = tournamentId,
                roundName = "Finals",
                matchOrder = 1
            )
            batch.set(finalMatchRef, finalMatch)

            // Semifinals
            val semi1Ref = matchesCollection.document()
            val semi1 = TournamentMatch(
                id = semi1Ref.id,
                tournamentId = tournamentId,
                roundName = "Semifinals",
                nextMatchId = finalMatchRef.id,
                matchOrder = 1
            )
            batch.set(semi1Ref, semi1)

            val semi2Ref = matchesCollection.document()
            val semi2 = TournamentMatch(
                id = semi2Ref.id,
                tournamentId = tournamentId,
                roundName = "Semifinals",
                nextMatchId = finalMatchRef.id,
                matchOrder = 2
            )
            batch.set(semi2Ref, semi2)

            // Quarterfinals
            val q1Ref = matchesCollection.document()
            val q1 = TournamentMatch(
                id = q1Ref.id,
                tournamentId = tournamentId,
                roundName = "Quarterfinals",
                team1Name = teams.getOrElse(0) { "TBD" },
                team2Name = teams.getOrElse(1) { "TBD" },
                nextMatchId = semi1Ref.id,
                matchOrder = 1
            )
            batch.set(q1Ref, q1)

            val q2Ref = matchesCollection.document()
            val q2 = TournamentMatch(
                id = q2Ref.id,
                tournamentId = tournamentId,
                roundName = "Quarterfinals",
                team1Name = teams.getOrElse(2) { "TBD" },
                team2Name = teams.getOrElse(3) { "TBD" },
                nextMatchId = semi1Ref.id,
                matchOrder = 2
            )
            batch.set(q2Ref, q2)

            val q3Ref = matchesCollection.document()
            val q3 = TournamentMatch(
                id = q3Ref.id,
                tournamentId = tournamentId,
                roundName = "Quarterfinals",
                team1Name = teams.getOrElse(4) { "TBD" },
                team2Name = teams.getOrElse(5) { "TBD" },
                nextMatchId = semi2Ref.id,
                matchOrder = 3
            )
            batch.set(q3Ref, q3)

            val q4Ref = matchesCollection.document()
            val q4 = TournamentMatch(
                id = q4Ref.id,
                tournamentId = tournamentId,
                roundName = "Quarterfinals",
                team1Name = teams.getOrElse(6) { "TBD" },
                team2Name = teams.getOrElse(7) { "TBD" },
                nextMatchId = semi2Ref.id,
                matchOrder = 4
            )
            batch.set(q4Ref, q4)
        }

        // Activate Tournament
        val tournamentRef = tournamentsCollection.document(tournamentId)
        batch.update(tournamentRef, "status", "ACTIVE")

        batch.commit().await()
    }

    suspend fun updateMatchScore(match: TournamentMatch, score1: Int, score2: Int, winnerName: String): Result<Unit> {
        return try {
            val batch = firestore.batch()
            val matchRef = matchesCollection.document(match.id)
            
            val updatedMatch = match.copy(
                score1 = score1,
                score2 = score2,
                winnerName = winnerName,
                isCompleted = true
            )
            batch.set(matchRef, updatedMatch)

            if (match.roundName == "Finals") {
                // Complete Tournament and declare overall champion!
                val tournamentRef = tournamentsCollection.document(match.tournamentId)
                batch.update(
                    tournamentRef, "status", "COMPLETED",
                    "winnerTeamName", winnerName
                )
            } else {
                // Advance champion to next linked match
                val nextMatchRef = matchesCollection.document(match.nextMatchId)
                val snapshot = nextMatchRef.get().await()
                if (snapshot.exists()) {
                    val isOdd = match.matchOrder % 2 != 0
                    if (isOdd) {
                        batch.update(nextMatchRef, "team1Name", winnerName)
                    } else {
                        batch.update(nextMatchRef, "team2Name", winnerName)
                    }
                }
            }

            batch.commit().await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
