package com.bay.aidemo

actual fun readLocalStorage(path: String): Map<String, String> {
    val result = mutableMapOf<String, String>()
    val keys = readLocalStorageKeys(path)
    for (i in 0..<keys.length) {
        if (keys[i] != null) {
            val key = keys[i].toString()
            readLocalStorageValue(key).also { result[key.removePrefix("$path:")] = it }
        }
    }
    return result.toMap()
}

actual fun writeLocalStorage(
    path: String,
    settings: Map<String, String>,
) {
    settings.forEach { (key, value) ->
        setLocalStorageKey("$path:$key", value)
    }
}

private fun setLocalStorageKey(
    key: String,
    value: String,
): Unit =
    js(
        """{
           localStorage.setItem(key, value)
        }""",
    )

private fun readLocalStorageKeys(prefix: String): JsArray<JsAny> =
    js(
        """
        (function(){
            let keys = []
            for(let i=0; i<localStorage.length; i++) {
               let key = localStorage.key(i) 
               if (key.startsWith(prefix)) {
                  keys.push(key)
               }
            }
            return keys;
        }) ()
    """,
    )

private fun readLocalStorageValue(key: String): String =
    js(
        """
        localStorage[key]
    """,
    )
