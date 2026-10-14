package com.proje.sosyal.user

enum class Role { USER, MODERATOR, ADMIN }

enum class UserStatus { ACTIVE, SUSPENDED, DELETED }

/** MINOR = 13-17, ADULT = 18+. Kullanıcı beyanıdır; doğrulama durumu ayrıca tutulur. */
enum class AgeGroup { MINOR, ADULT }

enum class AgeVerificationStatus { UNVERIFIED, PENDING, VERIFIED, REJECTED }

enum class ProfileVisibility { PUBLIC, PRIVATE }
