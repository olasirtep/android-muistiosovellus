package fi.hyria.simplememo

import android.app.AlertDialog
import android.content.DialogInterface
import android.content.Intent
import android.database.Cursor
import android.net.Uri
import android.os.Bundle
import android.provider.OpenableColumns
import android.view.View
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import java.io.InputStream
import java.io.OutputStream


// Näillä arvoilla erotellaan liittyykö tiedostohallintaohjelmalta saatu
// vastaus tiedoston tallentamiseen vai avaamiseen
const val REQUEST_CODE_WRITE = 1;
const val REQUEST_CODE_READ = 2;

// Avoinna olevan tiedoston Uri
var currentFileUri : Uri? = null

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        // Asetetaan title-teksti oletustekstiin
        supportActionBar?.title = getString(R.string.new_file)
    }

    // Tämä on liitetty tallennusnappiin
    fun saveToFile(view: View) {
        // Tarkistetaan onko meillä tiedosto auki vai tallennetaanko uutta tiedostoa
        if (currentFileUri != null) {
            // Jos tiedosto auki tallennetaan muutokset nykyiseen tiedostoon
            byteWriter(
                currentFileUri,
                findViewById<EditText>(R.id.noteContent).text.toString().toByteArray()
            )
        }
        else {
            // Jos kyseessä on uusi tiedosto avataan dialogi
            val intent = Intent(Intent.ACTION_CREATE_DOCUMENT)
            intent.setType("text/plain") // Vain tekstitiedostoja
            intent.addCategory(Intent.CATEGORY_OPENABLE) // Hakemiston täytyy olla sovelluksen kirjoitettavissa
            startActivityForResult(intent, REQUEST_CODE_WRITE)
        }
    }

    // Tämä on liitetty avausnappiin
    fun openFile(view: View) {
        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT)
        intent.setType("text/plain") // Vain tekstitiedostoja
        intent.addCategory(Intent.CATEGORY_OPENABLE) // Hakemiston täytyy olla sovelluksen luettavissa
        startActivityForResult(intent, REQUEST_CODE_READ)
    }

    // Metodi tallentaa tekstin tiedostoon käyttäen OutputStreamia
    fun byteWriter(uri: Uri?, bytes: ByteArray) {
        // Avataan OutputStream
        val output: OutputStream? = uri?.let {
            applicationContext.contentResolver.openOutputStream(
                it
            )
        }
        if (output != null) {
            output.write(bytes) // Kirjoitetaan tavut tiedostoon
            output.flush() // Tämä varmistaa, että tiedot on tallennettu kohdetiedostoon, eivätkä vain jääneet välimuistiin
            output.close() // Suljetaan tiedostovirta
        }
    }

    // Tämä metodi aktivoituu, kun käyttäjä palautuu tiedostodialogilta
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        // Metodi ylikirjoittaa onActivityResultin, joten kutsutaan AppCompatActivityn metodia
        super.onActivityResult(requestCode, resultCode, data)

        // Tarkistetaan saatiinko dialogilta onnistunut tulos
        if (resultCode == RESULT_OK) {
            // Päivitetään muuttujaan tiedoston Uri
            currentFileUri = data?.data
            // Haetaan tiedostonimi
            val filename = currentFileUri?.let { getFileName(it) }.toString()
            // Päivitetöön title
            supportActionBar?.title = filename

            // Tarkistetaan palautuduttiinko tallennus- vai avausdialogista
            if (requestCode == REQUEST_CODE_WRITE) {
                byteWriter(
                    currentFileUri,
                    findViewById<EditText>(R.id.noteContent).text.toString().toByteArray()
                )
            } else if (requestCode == REQUEST_CODE_READ) {
                // Avataan InputStream saatuun Uriin
                val input: InputStream? = currentFileUri?.let {
                    applicationContext.contentResolver.openInputStream(
                        it
                    )
                }

                // Jos avaus onnistui, luetaan tiedoston sisältö ja asetetaan se tekstikenttään
                if (input != null) {
                    val content = input.bufferedReader().use {
                        it.readText()
                    }
                    findViewById<EditText>(R.id.noteContent).setText(content)
                    input.close()
                }
            }
        }
    }

    // Palauttaa tiedoston nimen Urin perusteella
    fun getFileName(uri: Uri): String? {
        var result: String? = null
        if (uri.scheme == "content") {
            val cursor: Cursor? = contentResolver.query(uri, null, null, null, null)
            try {
                if (cursor != null && cursor.moveToFirst()) {
                    result = cursor.getString(cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME))
                }
            } finally {
                cursor?.close()
            }
        }
        if (result == null) {
            result = uri.path
            val cut = result!!.lastIndexOf('/')
            if (cut != -1) {
                result = result.substring(cut + 1)
            }
        }
        return result
    }


    // Liitetty uuden muistiinpanon nappiin
    fun newFile(view: View) {
        // Luodaan varoitusdialogi, jossa varmistetaan, että käyttäjän oli tarkoitus luoda uusi
        // tiedosto ja jättää nykyiset muutokset tallentamatta
        val builder: AlertDialog.Builder = AlertDialog.Builder(this@MainActivity)
        builder.setTitle(getString(R.string.new_file_alert_title))
        builder.setMessage(getString(R.string.new_file_alert_text))

        val dialogClickListener =
            DialogInterface.OnClickListener { dialog, which ->
                when (which) {
                    DialogInterface.BUTTON_POSITIVE -> {
                        // Tyhjennetään tekstikentän sisältö
                        findViewById<EditText>(R.id.noteContent).setText("")
                        // Poistetaan avoimen tiedoston Uri
                        currentFileUri = null
                        // Vaihdetaan ActionBar title ilmaisemaan uutta tiedostoa
                        supportActionBar?.title = getString(R.string.new_file)
                    }
                    DialogInterface.BUTTON_NEGATIVE -> {
                        Toast.makeText(this, getString(R.string.new_file_canceled), Toast.LENGTH_SHORT).show()
                    }
                }
            }

        builder.setPositiveButton(getString(R.string.yes), dialogClickListener)
        builder.setNegativeButton(getString(R.string.no), dialogClickListener)
        builder.create().show()
    }
}
