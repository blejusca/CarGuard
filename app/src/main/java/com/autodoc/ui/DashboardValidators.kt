package com.autodoc.ui.validators

import java.time.LocalDate

fun validateCarInput(
    brand: String,
    model: String,
    plate: String,
    yearText: String,
    engine: String,
    ownerName: String,
    ownerPhone: String,
    ownerEmail: String
): String {
    val errors = mutableListOf<String>()

    val cleanBrand = brand.trim()
    val cleanModel = model.trim()
    val cleanPlate = plate.trim().uppercase()
    val cleanYear = yearText.trim()
    val cleanEngine = engine.trim()
    val cleanOwnerName = ownerName.trim()
    val cleanOwnerPhone = ownerPhone.trim()
    val cleanOwnerEmail = ownerEmail.trim()

    val currentYear = LocalDate.now().year
    val maxAllowedYear = currentYear + 1
    val parsedYear = cleanYear.toIntOrNull()

    if (cleanBrand.isBlank()) {
        errors.add("Marca este obligatorie.")
    } else if (!isValidBrand(cleanBrand)) {
        errors.add("Marca nu este valida. Foloseste doar litere, spatii sau cratima, maximum 25 caractere.")
    }

    if (cleanModel.isBlank()) {
        errors.add("Modelul este obligatoriu.")
    } else if (!isValidModel(cleanModel)) {
        errors.add("Modelul nu este valid. Foloseste litere, cifre, spatii sau cratima, maximum 25 caractere.")
    }

    if (cleanPlate.isBlank()) {
        errors.add("Numarul de inmatriculare este obligatoriu.")
    } else if (!isValidPlate(cleanPlate)) {
        errors.add("Numarul de inmatriculare nu este valid. Foloseste 3-12 caractere: litere, cifre, spatii sau cratima.")
    }

    if (cleanYear.isBlank()) {
        errors.add("Anul fabricatiei este obligatoriu.")
    } else if (parsedYear == null) {
        errors.add("Anul fabricatiei trebuie sa fie numeric.")
    } else if (parsedYear !in 1950..maxAllowedYear) {
        errors.add("Anul fabricatiei trebuie sa fie intre 1950 si $maxAllowedYear.")
    }

    if (cleanEngine.isNotBlank() && !isValidEngine(cleanEngine)) {
        errors.add("Motorizarea nu este valida. Exemple acceptate: 1.6 TDI, 2.0 benzina, electric, hybrid.")
    }

    if (cleanOwnerName.isNotBlank() && !isReasonableName(cleanOwnerName)) {
        errors.add("Numele clientului nu este valid. Foloseste minimum 2 caractere si fara cifre.")
    }

    // Validare telefon internationala - accepta 8-15 cifre
    if (cleanOwnerPhone.isNotBlank() && !isValidPhone(cleanOwnerPhone)) {
        errors.add("Telefonul clientului nu este valid. Trebuie sa contina intre 8 si 15 cifre.")
    }

    // Validare email permisiva internationala - orice TLD de minimum 2 caractere
    if (cleanOwnerEmail.isNotBlank() && !isValidEmail(cleanOwnerEmail)) {
        errors.add("Emailul clientului nu este valid sau pare scris gresit.")
    }

    return errors.joinToString(separator = "\n")
}

private fun isValidBrand(value: String): Boolean {
    return Regex("""^[A-Za-zÀ-ž -]{2,25}$""").matches(value.trim())
}

private fun isValidModel(value: String): Boolean {
    return Regex("""^[A-Za-zÀ-ž0-9 -]{1,25}$""").matches(value.trim())
}

private fun isValidPlate(value: String): Boolean {
    return Regex("""^[A-Z0-9 -]{3,12}$""").matches(value.trim().uppercase())
}

private fun isValidEngine(value: String): Boolean {
    val cleanValue = value.trim().lowercase()

    if (cleanValue in listOf("electric", "hibrid", "hybrid", "benzina", "diesel", "gpl", "gaz")) {
        return true
    }

    val pattern = Regex("""^[0-9](\.[0-9])? ?[A-Za-zÀ-ž0-9 -]{0,15}$""")

    return pattern.matches(cleanValue) && cleanValue.length <= 20
}

/**
 * Validare telefon internationala.
 * Accepta orice numar cu 8-15 cifre dupa eliminarea separatorilor standard.
 * Acopera Romania, Danemarca, Germania, UK, Franta si orice alta tara.
 */
private fun isValidPhone(value: String): Boolean {
    val digits = value.trim()
        .replace(" ", "")
        .replace("-", "")
        .replace("(", "")
        .replace(")", "")
        .replace("+", "")
        .filter { it.isDigit() }

    return digits.length in 8..15
}

/**
 * Validare email internationala permisiva.
 * Accepta orice TLD de minimum 2 caractere - acopera .ro, .dk, .com, .fr, .uk, .de, .nl etc.
 * Blocheaza doar typo-urile evidente (ex: .coom, @yaoo.).
 */
private fun isValidEmail(value: String): Boolean {
    val email = value.trim().lowercase()
    val emailPattern = Regex("""^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\.[A-Za-z]{2,}$""")

    if (!emailPattern.matches(email)) {
        return false
    }

    val domain = email.substringAfter("@", missingDelimiterValue = "")
    val tld = domain.substringAfterLast(".", missingDelimiterValue = "")

    // TLD minim 2 caractere
    if (tld.length < 2) return false

    // Nu accepta domenii cu puncte consecutive
    if (domain.contains("..")) return false

    // Blocheaza doar typo-uri comune evidente
    val suspiciousParts = listOf(
        "..",
        ".,",
        ",.",
        ".coom",
        ".comm",
        "@yaoo.",
        "@yahooo.",
        "@yahho.",
        "@gmai.",
        "@gmial.",
        "@hotmial.",
        "@outlok."
    )

    return suspiciousParts.none { email.contains(it) }
}

private fun isReasonableName(value: String): Boolean {
    return Regex("""^[A-Za-zÀ-ž -]{2,40}$""").matches(value.trim())
}
