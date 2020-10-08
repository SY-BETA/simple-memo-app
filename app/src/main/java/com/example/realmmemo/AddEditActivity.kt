package com.example.realmmemo

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.view.MenuItem
import androidx.appcompat.app.AppCompatActivity
import io.realm.Realm
import kotlinx.android.synthetic.main.activity_add_edit.*
import java.util.*

/**
 * メモの新規追加と修正をする Activity
 * MainActivity -> AddEditActivity
 */
class AddEditActivity : AppCompatActivity() {

    private val realm = Realm.getDefaultInstance()

    // MainActivityからintentで遷移した来た時にデータを受け取る
    // null -> 新規追加;  not null -> 編集
    private var memoID: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_add_edit)

        // ActionBarに戻るボタンを表示
        supportActionBar?.apply {
            setDisplayHomeAsUpEnabled(true)
            setHomeButtonEnabled(true)
        }

        // intentからmemoIDを取得
        memoID = intent.getStringExtra("memoID")

        // memoIDがnull出ない場合は、既存メモの編集なので、
        // メモモデルを検索、取得し、それをビューに反映させます
        // 「?」セーフコール演算子とスコープ関数でnull のときは波カッコ内の処理を実行しないという風にしています
        memoID?.also {
            val memoModel = getMemoByID(it)
            memoEditText.setText(memoModel?.memo)
        }
    }

    // 戻るボタン(androidの画面下部システムの戻るボタン)を押したときの処理
    // このタイミングでメモを保存します
    override fun onBackPressed() {
        // superクラスのバックボタン処理はコメントアウト finish()で手動で終了させることでactivityResultを受け取れるようにする
//        super.onBackPressed()
        // 引数に保存するメモ内容を渡しています
        // また、EditTextに 空文字チェックを入れています
        if (!memoEditText.text.isNullOrEmpty()) {
            saveMemo(memoEditText.text.toString())
        } else {
            // 空白の際は何もせずにリストへ戻る
            super.onBackPressed()
        }
    }

    // onOptionsItemSelected()では、ActionBarにあるボタンなどをクリックしたときにコールされる処理を書きます
    // 今回では、ActionBar左の 「←」 をクリックしたときの処理です
    // 以前、資料でお伝えしましたが、↓みたいな override fun ~ は 「onOpt」とかくらいまで入力したら自動予測変換が
    // サジェスチョンされるのでそれを使用しましょう
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        // item.itemId でクリックしたビューのIDを取得しwhen式でケースごとに処理します
        return when (item.itemId) {
            android.R.id.home -> {
                // android.R.~とつくもの(MainActivityでも出てきました)はandroid studioにデフォルトで要されている
                // リソースファイル(レイアウトファイルなどの ~xmlファイルで定義されるもの)のことです
                // android.R.id.home はデフォルトで用意されている 「←」　ボタンのビューIDです
                // super.onBackPressed()で戻るボタン(androidの画面下部システムの戻るボタン)を押したときと同じ処理をさせます
                //　コード的には次の処理は上に定義してるoverride onBackPressed() の方に行きます
                // 当然ですが、ここを書いていないと、onCreate()で戻るボタンをActionBarに表示しましたが、押しても何も起きません
                onBackPressed()
                true
            }
            else -> false
        }
    }

    // IDから検索しメモモデルを取得する
    private fun getMemoByID(id: String): MemoModel? {
        return realm.where(MemoModel::class.java).equalTo("id", id).findFirst()
    }

    // メモモデルを保存する
    private fun saveMemo(memo: String) {
        if (memoID == null) {
            /** Activity遷移時に取得したmemoIDデータがnullの時は新規追加(insert)**/

            // realmではinsert, update処理はすべてトランザクション処理で行わなければいけません
            realm.executeTransaction { realm ->
                // realmオブジェクトを新規に生成
                val memoModel = realm.createObject(
                    MemoModel::class.java,
                    UUID.randomUUID().toString()
                )
                // realmのMemoModelオブジェクトのmemoカラムをセットします
                memoModel.apply {
                    this.memo = memo
                }
                // これで新規メモの保存(insert完了です)
            }
            // 続いて、MainActivityに戻るときにresultとしてIDを渡します
            val intent = Intent().apply {
                putExtra("memo", memo)
            }
            setResult(Activity.RESULT_OK, intent)
            finish()
        } else {
            /** Activity遷移時に取得したmemoIDデータが存在するときは既存のメモデータの編集(update)**/

            realm.executeTransaction { realm ->
                // 遷移時に渡されたIDからデータベースに存在するメモモデルを取得し保存(update)
                val memoModel = realm.where(MemoModel::class.java)
                    .equalTo("id", memoID)
                    .findFirst()
                memoModel?.apply {
                    this.memo = memo
                }
                // これで既存メモの保存(update)完了です
            }
            // 続いて、MainActivityに戻るときにresultとしてIDを渡します
            val intent = Intent().apply {
                putExtra("memo", memo)
                putExtra("clickedPos", intent.getIntExtra("clickedPos", 0))
            }
            setResult(Activity.RESULT_OK, intent)
            finish()
        }
    }
}