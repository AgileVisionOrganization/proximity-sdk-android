package distance.agilevision.com.beacondistanceapp

/**
 * @author Andrew Koidan, AgileVision, 25.01.18.
 */

class Identifier( var namespace: String, var instance: String) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as Identifier

        if (namespace != other.namespace) return false
        if (instance != other.instance) return false

        return true
    }

    override fun hashCode(): Int {
        var result = namespace.hashCode()
        result = 31 * result + instance.hashCode()
        return result
    }
}