package ink.abb.pogo.api.cache

import POGOProtos.Enums.PokemonIdOuterClass
import POGOProtos.Map.Fort.FortDataOuterClass
import POGOProtos.Map.Pokemon.MapPokemonOuterClass
import POGOProtos.Map.Pokemon.WildPokemonOuterClass
import ink.abb.pogo.api.PoGoApi

class MapPokemon {
    val encounterKind: EncounterKind

    val spawnPointId: String

    val encounterId: Long

    val pokemonId: PokemonIdOuterClass.PokemonId

    val pokemonIdValue: Int

    val expirationTimestampMs: Long

    val latitude: Double

    val longitude: Double

    val poGoApi: PoGoApi

    val valid: Boolean
        get() = poGoApi.currentTimeMillis() < expirationTimestampMs

    constructor(poGoApi: PoGoApi, proto: MapPokemonOuterClass.MapPokemon) {
        this.encounterKind = EncounterKind.NORMAL
        this.spawnPointId = proto.spawnPointId
        this.encounterId = proto.encounterId
        this.pokemonId = proto.pokemonId
        this.pokemonIdValue = proto.pokemonIdValue
        this.expirationTimestampMs = proto.expirationTimestampMs
        this.latitude = proto.latitude
        this.longitude = proto.longitude
        this.poGoApi = poGoApi
    }

    constructor(poGoApi: PoGoApi, proto: WildPokemonOuterClass.WildPokemon) {
        this.encounterKind = EncounterKind.NORMAL
        this.spawnPointId = proto.spawnPointId
        this.encounterId = proto.encounterId
        this.pokemonId = proto.pokemonData.pokemonId
        this.pokemonIdValue = proto.pokemonData.pokemonIdValue
        this.expirationTimestampMs = proto.timeTillHiddenMs.toLong()
        this.latitude = proto.latitude
        this.longitude = proto.longitude
        this.poGoApi = poGoApi

    }

    constructor(poGoApi: PoGoApi, proto: FortDataOuterClass.FortData) {
        this.spawnPointId = proto.lureInfo.fortId
        this.encounterId = proto.lureInfo.encounterId
        this.pokemonId = proto.lureInfo.activePokemonId
        this.pokemonIdValue = proto.lureInfo.activePokemonIdValue
        this.expirationTimestampMs = proto.lureInfo
                .lureExpiresTimestampMs
        this.latitude = proto.latitude
        this.longitude = proto.longitude
        this.encounterKind = EncounterKind.DISK
        this.poGoApi = poGoApi
    }


    enum class EncounterKind {
        NORMAL,
        DISK
    }

    override fun toString(): String {
        return "MapPokemon(encounterKind=$encounterKind, spawnPointId='$spawnPointId', encounterId=$encounterId, pokemonId=$pokemonId, pokemonIdValue=$pokemonIdValue, expirationTimestampMs=$expirationTimestampMs, latitude=$latitude, longitude=$longitude)"
    }


}