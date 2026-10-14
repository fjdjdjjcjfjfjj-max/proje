package com.proje.sosyal.user

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import java.util.UUID

interface UserRepository : JpaRepository<User, UUID> {
    /** Kullanıcı adları küçük harfle saklanır; çağıran önce normalize etmelidir. */
    fun findByUsername(username: String): User?

    fun existsByUsername(username: String): Boolean

    @Query("select u from User u where lower(u.email) = lower(:email)")
    fun findByEmailIgnoreCase(email: String): User?

    @Query("select count(u) > 0 from User u where lower(u.email) = lower(:email)")
    fun existsByEmailIgnoreCase(email: String): Boolean
}
