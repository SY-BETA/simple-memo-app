package com.example.realmmemo

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.activity.result.ActivityResult
import androidx.activity.result.contract.ActivityResultContracts.StartActivityForResult
import androidx.appcompat.app.AppCompatActivity
import io.realm.Realm
import io.realm.RealmResults
import kotlinx.android.synthetic.main.activity_main.*

/**
 * メモ一覧を表示する Activity
 *
 * このアプリではRealmというORMapperライブラリを使用します。Gradleの設定などが必要となるので、ドキュメントなどで各自ご確認ください
 *
 * 今回、初心者向け資料としてコメントをとても多く書くようにしていますが、
 * 実際開発中にはここまでコメントを書くのはかえって可読性を損ないとても良くないです。マネしないようにしてください
 * まったくコメントが無いのも良くないですが、ここまでたくさん書くのも良くないです
 * すでに知ってることやいらないコメントなどは適宜削除してください
 * このことを頭に入れながら、コードを見てください
 */
class MainActivity : AppCompatActivity() {

    private var realm: Realm? = null

    // listViewのアダプタ (ビューに表示するデータの管理を担当)
    private var adapter: ArrayAdapter<String>? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // realmの初期化。 thisを渡すのではなくapplicationContextを渡してください
        Realm.init(applicationContext)
        realm = Realm.getDefaultInstance()

        // realmデータベースの初期化とかの前にリスト表示だけ先に試してみたい場合に使ってください
//        setupTestList()

        // データベースからすべてのメモデータをリストとして取得します
        // 「?:」という見慣れない記号ですが、めちゃくちゃ便利ですので覚えておいてください
        // smart castと呼ばれるもので、 ?:の左側がnull のときに?:の右側のデータを返す機能を提供します
        // 何がいいかというと null を防ぐことができ、 null pointer対策になります
        // 多分後々便利だなーって実感してくるようになります
        val memoList = getMemoList() ?: listOf()

        // リスト表示するためにListViewというビューを使用します
        // シンプルにするためにリストビューを使用しましたが、凝ったレイアウトのリストを作りたい場合はRecyclerViewを使用します
        // RecyclerViewはAdapter, clickListenerなど自作する必要があるため難易度が高めです。しかし、レイアウトの自由度、パフォーマンスが高いです
        // たとえば、Lineのチャット画面とかもRecyclerViewを使えば作れます
        // RecyclerView + kotlinとかで調べるとたくさん記事出てくるので試したい方はやってみてください
        // ArrayAdapterにはコンテキスト、レイアウトファイル(リストの１つのitemのレイアウト)、表示するリストデータを渡します
        // android.R.simple_list_item_1とはデフォルトで用意されているレイアウトファイルです
        // simple_list_item_1のところで 「ctrl + B」を押すとそのレイアウトファイルが見れます(textViewがあるだけの簡単なレイアウトです)
        adapter = ArrayAdapter(this, android.R.layout.simple_list_item_1, memoList.map { it.memo })
        memoListView.adapter = adapter

        // 今回別のActivity(メモの編集画面)に遷移しこの画面に戻ってきたときにメモリストを更新するための準備をしておきます
        // startActivity(intent)にコールバックを受け取れるようにしたstartActivityForResult(intent, code)を使用します
        // しかし、startActivityForResultが非推奨になる予定なので新しいAPIを使います
        // 結構古い情報などが多いので気を付けて下さい
        // 下のメソッドが赤線とかで出てこない場合はgradleに以下を追加してください
        // implementation 'androidx.activity:activity-ktx:1.2.0-beta01'
        // implementation 'androidx.fragment:fragment-ktx:1.3.0-beta01'
        // 2020/09時点の情報なので今後変わってる可能性があります、その際は適宜変えてください
        val memoEditLauncher =
            registerForActivityResult(StartActivityForResult()) { result: ActivityResult? ->
                // 遷移先のアクティビティから戻ってきたときに以下に書くコードが実行されます
                if (result?.resultCode == Activity.RESULT_OK) {
                    Toast.makeText(this, "AddEditActivityから戻ってきた(UPDATE)!", Toast.LENGTH_SHORT)
                        .show()
                    result.data?.apply {
                        val memo = getStringExtra("memo")
                        val pos = getIntExtra("clickedPos", 0)
                        adapter?.remove(adapter?.getItem(pos))
                        adapter?.insert(memo, pos)
                    }
                }
            }
        // メモリストクリックで画面遷移するようにリスナをセット
        /** メモの編集 **/
        memoListView.setOnItemClickListener { parent, view, position, id ->
            // 当然、最初は遷移先のアクティビティとか用意できていないのでtoastとかで随時デバッグしつつ開発
//            Toast.makeText(this, "クリック: $position", Toast.LENGTH_SHORT).show()

            // 以前資料で学んだように、intentを使って別Activityに遷移します
            // 今回異なるのは startActivity(intent)ではなくregisterForActivityResultを使用しているところです
            // これを使えば、遷移先のActivityから戻ってきたときにコールバックとして何かしらの処理をかけます
            // 例えば、 AddEditActivityでメモを保存し、一覧画面(MainActivity)に戻ってきたときにリストのデータ更新処理をするなど
            val intent = Intent(this, AddEditActivity::class.java)
            // putExtraでString情報(クリックしたメモのID)とpositionを遷移先のアクティビティに渡します
            intent.putExtra("memoID", memoList[position].id)
            intent.putExtra("clickedPos", position)

            // いろいろと調べると↓のメソッドが出てくるかもしれません
            // こちらは今後非推奨となるので使用しないようにしてください
//            startActivityForResult(intent, CODE_MEMO_EDIT)

            // intentを使用してAddEditActivityに遷移
            memoEditLauncher.launch(intent)
        }

        val memoAddLauncher =
            registerForActivityResult(StartActivityForResult()) { result: ActivityResult? ->
                // 遷移先のアクティビティから戻ってきたときに以下に書くコードが実行されます
                Toast.makeText(this, "AddEditActivityから戻ってきた(INSERT)!", Toast.LENGTH_SHORT)
                    .show()
                if (result?.resultCode == Activity.RESULT_OK) {
                    result.data?.apply {
                        // 遷移先のアクティビティからresultとしてわたってくるメモデータを取得し、メモリストのビューを差分更新します
                        val memo = getStringExtra("memo")
                        adapter?.add(memo)  // adapter.add(String)でListViewにデータ追加しビューを更新できます
                    }
                }
            }
        // 画面右下のFABクリックで画面遷移するようにリスナをセット
        /** メモを新規追加 **/
        memoAddFab.setOnClickListener {
            // 上と同じようにregisterForActivityResultで遷移させます
            // 異なる点はputExtra等でなにもデータを渡さないところです
            // 遷移先のアクティビティでデータが入ってるか、null かで新規追加か、編集かを判断します
            val intent = Intent(this, AddEditActivity::class.java)
            // intentを使用してAddEditActivityに遷移
            memoAddLauncher.launch(intent)
        }
    }

    // Realmデータベースからメモモデルデータをすべて取得
    // RealmResults<モデルクラス名>というrealm独自の型です
    private fun getMemoList(): RealmResults<MemoModel>? =
        realm?.where(MemoModel::class.java)?.findAll()

    // とりあえずrealmとか関係なくリスト表示を試してみたい場合は、
    // 以下のようにテストリストを用意しadapterをセットするとリストが表示されます
    private fun setupTestList() {
        val testArray = List(20) { "$it" }
        adapter = ArrayAdapter(this, android.R.layout.simple_list_item_1, testArray)
        memoListView.adapter = adapter
    }

    // メモクリックでstartActivityForResultで遷移し戻ってきたときのコールバックを受け取る処理
    // startForActivityResultとかを検索したら以下でコールバックを受け取るように書いてある記事を見つけた方いるかもしれません
    // 以下のメソッドはstartForActivityResultと同様に今後非推奨となるので使用しないようにしてください
    // resultをコールバックとして受け取る処理はregisterActivityResultを使用してください
//    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
//        super.onActivityResult(requestCode, resultCode, data)
//    }
}