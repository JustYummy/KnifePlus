package io.github.justyummy.knifeplus

import android.app.Activity
import android.app.AlertDialog
import android.content.DialogInterface
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.WindowManager
import android.widget.EditText
import android.widget.ImageButton
import android.widget.Toast

import io.github.justyummy.knife.KnifeText

class MainActivity : Activity() {
    private var knife: KnifeText? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        knife = findViewById(R.id.knife)

        knife!!.fromHtml(EXAMPLE)

        setupBold()
        setupItalic()
        setupUnderline()
        setupStrikethrough()
        setupBullet()
        setupQuote()
        setupLink()
        setupClear()
        setupNumbered()    //ADD

        showHtmlText()

    }

    private fun showHtmlText() {

        val underline = findViewById<View>(R.id.show_html) as ImageButton

        underline.setOnClickListener {
            //                String text = knife.toString();
            val text = knife!!.toHtml()
            //                Log.d("TEXT", text);
            Toast.makeText(this@MainActivity, text, Toast.LENGTH_LONG).show()
        }

        underline.setOnLongClickListener {
            Toast.makeText(this@MainActivity, "Show Html", Toast.LENGTH_SHORT).show()
            true
        }
    }

    private fun setupBold() {
        val bold = findViewById<View>(R.id.bold) as ImageButton

        bold.setOnClickListener { knife!!.bold(!knife!!.contains(KnifeText.FORMAT_BOLD)) }

        bold.setOnLongClickListener {
            Toast.makeText(this@MainActivity, R.string.toast_bold, Toast.LENGTH_SHORT).show()
            true
        }
    }

    private fun setupItalic() {
        val italic = findViewById<View>(R.id.italic) as ImageButton

        italic.setOnClickListener { knife!!.italic(!knife!!.contains(KnifeText.FORMAT_ITALIC)) }

        italic.setOnLongClickListener {
            Toast.makeText(this@MainActivity, R.string.toast_italic, Toast.LENGTH_SHORT).show()
            true
        }
    }

    private fun setupUnderline() {
        val underline = findViewById<View>(R.id.underline) as ImageButton

        underline.setOnClickListener { knife!!.underline(!knife!!.contains(KnifeText.FORMAT_UNDERLINED)) }

        underline.setOnLongClickListener {
            Toast.makeText(this@MainActivity, R.string.toast_underline, Toast.LENGTH_SHORT).show()
            true
        }
    }

    private fun setupStrikethrough() {
        val strikethrough = findViewById<View>(R.id.strikethrough) as ImageButton

        strikethrough.setOnClickListener { knife!!.strikethrough(!knife!!.contains(KnifeText.FORMAT_STRIKETHROUGH)) }

        strikethrough.setOnLongClickListener {
            Toast.makeText(this@MainActivity, R.string.toast_strikethrough, Toast.LENGTH_SHORT).show()
            true
        }
    }

    private fun setupBullet() {
        val bullet = findViewById<View>(R.id.bullet) as ImageButton

        bullet.setOnClickListener { knife!!.bullet(!knife!!.contains(KnifeText.FORMAT_BULLET)) }


        bullet.setOnLongClickListener {
            Toast.makeText(this@MainActivity, R.string.toast_bullet, Toast.LENGTH_SHORT).show()
            true
        }
    }

    //Numbered
    private fun setupNumbered() {
        val numbered = findViewById<View>(R.id.numbered) as ImageButton

        numbered.setOnClickListener {
            //                Log.d("DDD", knife.toHtml());
            knife!!.numbered(!knife!!.contains(KnifeText.FORMAT_NUMBERED))
        }


        numbered.setOnLongClickListener {
            Toast.makeText(this@MainActivity, R.string.toast_numbered, Toast.LENGTH_SHORT).show()
            true
        }
    }

    private fun setupQuote() {
        val quote = findViewById<View>(R.id.quote) as ImageButton

        quote.setOnClickListener { knife!!.quote(!knife!!.contains(KnifeText.FORMAT_QUOTE)) }

        quote.setOnLongClickListener {
            Toast.makeText(this@MainActivity, R.string.toast_quote, Toast.LENGTH_SHORT).show()
            true
        }
    }

    private fun setupLink() {
        val link = findViewById<View>(R.id.link) as ImageButton

        link.setOnClickListener { showLinkDialog() }

        link.setOnLongClickListener {
            Toast.makeText(this@MainActivity, R.string.toast_insert_link, Toast.LENGTH_SHORT).show()
            true
        }
    }

    private fun setupClear() {
        val clear = findViewById<View>(R.id.clear) as ImageButton

        clear.setOnClickListener { knife!!.clearFormats() }

        clear.setOnLongClickListener {
            Toast.makeText(this@MainActivity, R.string.toast_format_clear, Toast.LENGTH_SHORT).show()
            true
        }
    }

    private fun showLinkDialog() {
        val builder = AlertDialog.Builder(this)
        builder.setCancelable(false)

        val view = layoutInflater.inflate(R.layout.dialog_link, null, false)
        val editTextTitle = view.findViewById<View>(R.id.title_link) as EditText
        val editTextLink = view.findViewById<View>(R.id.edit_link) as EditText

        builder.setView(view)
        builder.setTitle(R.string.dialog_title)

        builder.setPositiveButton(R.string.dialog_button_ok) { dialog, which ->
            val title = editTextTitle.text.toString().trim { it <= ' ' }
            val link = editTextLink.text.toString().trim { it <= ' ' }

            // When KnifeText lose focus, use this method
            knife!!.link(title, link)
        }

        builder.setNegativeButton(R.string.dialog_button_cancel) { dialog, which ->
            // DO NOTHING HERE
        }

        val alertDialog = builder.create()

        // pop up keyboard
        editTextTitle.requestFocus()
        alertDialog.window!!.clearFlags(WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_ALT_FOCUSABLE_IM)
        alertDialog.window!!.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE)

        alertDialog.show()
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_main, menu)
        return super.onCreateOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.undo -> knife!!.undo()
            R.id.redo -> knife!!.redo()
            R.id.github -> {
                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(resources.getString(R.string.app_repo)))
                startActivity(intent)
            }
            else -> {
            }
        }

        return true
    }

    companion object {
        private const val BOLD = "<b>Bold</b><br><br>"
        private const val ITALIT = "<i>Italic</i><br><br>"
        private const val UNDERLINE = "<u>Underline</u><br><br>"
        private const val STRIKETHROUGH = "<s>Strikethrough</s><br><br>" // <s> or <strike> or <del>
        private const val BULLET = "<ul><li>Coffee</li><li>Tea</li></ul>"
        private const val QUOTE = "<blockquote>Quote</blockquote>"
        private const val LINK = "<a href=\"https://github.com/mthli/Knife\">Link</a><br><br>"
        private const val MY = "<span style=\"color:#000000;\">文字顏色為Black</span><br><br>"
        //    private static final String MY = "<span style=\"color:#000000;\"><span style=\"background-color:#FFFF00\">文字背景顏色為黄色</span></span><br><br>";
        private const val My2 = "<big>BIG</big><br><br>"
        private const val My3 = "<small>Small</small><br><br>"
        private const val My4 = "<h1>HEADER1</h1><br><br>"
        private const val My5 = "<ul><li>Coffee</li><li>Tea</li><li>Milk</li></ul><br><br>"
        private const val My6 = "<ol><li>No 1</li><li>No 2</li><li>No 3</li><li>No 4</li></ol><br>"
        // Bullet     <ul><li>    </li></ul>   ->  <ul><bu>   </bu></ul>
        // Numbered   <ol><li>    </li></ol>   ->  <ol><nu>   </nu></ol>
        private const val EXAMPLE =
            BOLD + ITALIT + UNDERLINE + STRIKETHROUGH + BULLET + QUOTE + LINK + MY + My2 + My3 + My4 + My5 + My6
    }
}
