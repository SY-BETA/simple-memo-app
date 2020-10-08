package com.example.realmmemo

import io.realm.RealmObject
import io.realm.annotations.PrimaryKey
import java.util.*

/**
 * メモのDBスキーマ
 * Excelのテーブル、カラムとかのことです
 *
 * Realmは内部でモデルクラスを継承するので継承可能である装飾しopenを必ずつけます
 */
open class MemoModel(
    // primaryKeyアノテーションで一意なキーであることを明示
    @PrimaryKey var id: String = UUID.randomUUID().toString(),
    var memo: String = ""
) : RealmObject()