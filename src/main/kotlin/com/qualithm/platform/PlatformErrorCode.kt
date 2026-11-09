package com.qualithm.platform

/**
 * The error codes this client version knows. The platform declares the set growable, so a code
 * added after this version decodes to [UNKNOWN], as does a failure with no response.
 *
 * @property value The code as the API sends it, such as `not_found`.
 */
public enum class PlatformErrorCode(public val value: String) {
    /** The request is malformed. */
    BAD_REQUEST("bad_request"),

    /** The token is missing, expired or revoked. */
    UNAUTHORIZED("unauthorized"),

    /** The token can't do this. */
    FORBIDDEN("forbidden"),

    /** The resource doesn't exist, or isn't visible to the token. */
    NOT_FOUND("not_found"),

    /** The request conflicts with the resource's current state. */
    CONFLICT("conflict"),

    /** A precondition on the request didn't hold. */
    PRECONDITION_FAILED("precondition_failed"),

    /** The request body's media type isn't accepted. */
    UNSUPPORTED_MEDIA_TYPE("unsupported_media_type"),

    /** The request is well formed but its contents are invalid. */
    UNPROCESSABLE_ENTITY("unprocessable_entity"),

    /** A request this one depends on failed. */
    FAILED_DEPENDENCY("failed_dependency"),

    /** Too many requests; retry later. */
    RATE_LIMITED("rate_limited"),

    /** The platform failed. */
    INTERNAL_ERROR("internal_error"),

    /** A backend the platform needs is unavailable; retry later. */
    BACKEND_UNAVAILABLE("backend_unavailable"),

    /** The change would leave the team without an owner. */
    LAST_OWNER("last_owner"),

    /** The role is higher than the caller's own. */
    ROLE_TOO_HIGH("role_too_high"),

    /** The email address is already in use. */
    EMAIL_IN_USE("email_in_use"),

    /** The one-time code is wrong or expired. */
    OTP_INVALID("otp_invalid"),

    /** The account still owns a team. */
    OWNS_TEAM("owns_team"),

    /** The account doesn't exist. */
    ACCOUNT_NOT_FOUND("account_not_found"),

    /** The account is already a member. */
    ALREADY_MEMBER("already_member"),

    /** The zone is closed. */
    ZONE_CLOSED("zone_closed"),

    /** The resource is in a different zone. */
    ZONE_MISMATCH("zone_mismatch"),

    /** The plan's limit is reached. */
    PLAN_LIMIT("plan_limit"),

    /** The registered-device limit is reached. */
    REGISTERED_LIMIT("registered_limit"),

    /** The name is already taken. */
    NAME_TAKEN("name_taken"),

    /** This needs a paid plan. */
    PAID_PLAN_REQUIRED("paid_plan_required"),

    /** The scope still holds resources. */
    SCOPE_NOT_EMPTY("scope_not_empty"),

    /** The idempotency key was reused with a different request. */
    IDEMPOTENCY_MISMATCH("idempotency_mismatch"),

    /** A request with this idempotency key is still in flight. */
    IDEMPOTENCY_IN_FLIGHT("idempotency_in_flight"),

    /** A code this version doesn't know, or a failure with no response. */
    UNKNOWN("unknown");

    /** Looks up a code by its wire value. */
    public companion object {
        private val byValue = entries.filter { it != UNKNOWN }.associateBy { it.value }

        /**
         * The code for [value], or [UNKNOWN] when it's missing or this version doesn't know it.
         *
         * @param value The code as the API sent it.
         */
        public fun fromValue(value: String?): PlatformErrorCode = byValue[value] ?: UNKNOWN
    }
}
