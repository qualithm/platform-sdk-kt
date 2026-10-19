package com.qualithm.platform.generated.infrastructure

import com.squareup.moshi.Moshi
import com.squareup.moshi.adapters.EnumJsonAdapter
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory

public object Serializer {
    @JvmStatic
    public val moshiBuilder: Moshi.Builder = Moshi.Builder()
        .add(OffsetDateTimeAdapter())
        .add(LocalDateTimeAdapter())
        .add(LocalDateAdapter())
        .add(UUIDAdapter())
        .add(ByteArrayAdapter())
        .add(URIAdapter())
        .add(KotlinJsonAdapterFactory())
        .add(BigDecimalAdapter())
        .add(BigIntegerAdapter())

    @JvmStatic
    public val moshi: Moshi by lazy {
        SerializerHelper.addEnumUnknownDefaultCase(moshiBuilder)
        moshiBuilder.build()
    }
}
