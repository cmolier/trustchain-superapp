package nl.tudelft.trustchain.offlineeuro.db

import android.content.Context
import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.android.AndroidSqliteDriver
import nl.tudelft.offlineeuro.sqldelight.Database
import nl.tudelft.offlineeuro.sqldelight.DepositedEurosQueries
import nl.tudelft.trustchain.offlineeuro.cryptography.BilinearGroup
import nl.tudelft.trustchain.offlineeuro.entity.DigitalEuro

class DepositedEuroManager(
    context: Context?,
    group: BilinearGroup,
    driver: SqlDriver = AndroidSqliteDriver(Database.Schema, context!!, "deposited_euros.db"),
) : OfflineEuroManager(group, driver) {
    private val queries: DepositedEurosQueries = database.depositedEurosQueries
    private val digitalEuroMapper = {
            serialNumber: String,
            firstTheta: ByteArray,
            signature: ByteArray,
            previousProofs: ByteArray?,
            ephemeralSignatures: ByteArray
        ->
        DigitalEuro(
            serialNumber,
            group.gElementFromBytes(firstTheta),
            deserializeSchnorr(signature)!!,
            deserializeGSP(previousProofs),
            deserializeSchnorrList(ephemeralSignatures)
        )
    }

    init {
        queries.createDepositedEurosTable()
        queries.clearDepositedEurosTable()
    }

    fun insertDigitalEuro(digitalEuro: DigitalEuro) {
        val (serialNumber, firstTheta1, signature, proofs) = digitalEuro
        queries.insertDepositedEuro(
            serialNumber,
            firstTheta1.toBytes(),
            serialize(signature)!!,
            serialize(proofs),
            serialize(digitalEuro.ephemeralKeySignatures)!!
        )
    }

    fun getDigitalEurosByDescriptor(digitalEuro: DigitalEuro): List<DigitalEuro> {
        val (serialNumber, firstTheta1, signature, _) = digitalEuro
        return queries.getDepositedEuroByDescriptor(
            serialNumber,
            firstTheta1.toBytes(),
            serialize(signature)!!,
            digitalEuroMapper
        ).executeAsList()
    }

    fun getAllDepositedEuros(): List<DigitalEuro> {
        return queries.getDepositedEuros(digitalEuroMapper).executeAsList()
    }

    fun clearDepositedEuros() {
        return queries.clearDepositedEurosTable()
    }
}
