package app.yuki.core.network

import app.yuki.core.model.AuthAccount
import kotlinx.serialization.Serializable

@Serializable
internal data class AccountDto(
    val id: String,
    val name: String = "",
    val email: String = "",
    val image: String? = null,
)

@Serializable
internal data class SignInResponseDto(
    val user: AccountDto? = null,
)

@Serializable
internal data class SessionResponseDto(
    val user: AccountDto? = null,
)

@Serializable
internal data class SignInRequestDto(
    val email: String,
    val password: String,
)

@Serializable
internal data class SignUpRequestDto(
    val name: String,
    val email: String,
    val password: String,
)

@Serializable
internal data class UpdateAccountRequestDto(
    val name: String,
)

@Serializable
internal data class SendVerificationRequestDto(
    val email: String,
)

@Serializable
internal data class AvatarUploadRequestDto(
    val contentType: String,
    val data: String,
)

@Serializable
internal data class AuthErrorDto(
    val code: String? = null,
    val message: String? = null,
)

internal fun AccountDto.toDomain(): AuthAccount = AuthAccount(
    id = id,
    name = name,
    email = email,
    imageUrl = image,
)
