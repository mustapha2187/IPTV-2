package com.musta.iptv.server

/** The small web page the phone opens. No scripts from outside, no tracking. */
object SetupPage {

    fun texts(lang: String): Map<String, String> = when (lang) {
        "ar" -> AR
        "ru" -> RU
        else -> EN
    }

    fun isRtl(lang: String) = lang == "ar"

    fun render(lang: String, template: String, pin: String, values: Map<String, String> = emptyMap()): String {
        var out = template
        for ((k, v) in texts(lang)) out = out.replace("{{" + k + "}}", v)
        for ((k, v) in values) out = out.replace("{{" + k + "}}", v)
        return out.replace("{{pin}}", pin).replace("{{dir}}", if (isRtl(lang)) "rtl" else "ltr")
    }

    const val FORM = """<!doctype html>
<html dir="{{dir}}"><head><meta charset="utf-8">
<meta name="viewport" content="width=device-width,initial-scale=1">
<title>Musta IPTV</title>
<style>
body{font-family:system-ui,sans-serif;margin:0;padding:20px;background:#0b0e14;color:#eaf0fa;max-width:520px;margin:auto}
h1{font-size:20px;font-weight:500}
label{display:block;margin:14px 0 6px;font-size:14px;color:#9aa7bc}
input,select,textarea{width:100%;box-sizing:border-box;padding:12px;font-size:16px;border-radius:8px;border:1px solid #2a3344;background:#151a23;color:#eaf0fa}
button{margin-top:22px;width:100%;padding:14px;font-size:16px;border:0;border-radius:8px;background:#3d8bff;color:#04122b;font-weight:600}
.hint{font-size:13px;color:#9aa7bc;margin-top:6px}
.err{background:#3a1414;padding:10px;border-radius:8px;margin-bottom:12px}
</style></head><body>
<h1>{{title}}</h1>
{{error}}
<form method="post" action="/add?k={{pin}}">
<label>{{type}}</label>
<select name="type" id="t" onchange="sw()">
<option value="m3u">M3U</option>
<option value="xtream">Xtream</option>
<option value="stalker">Stalker / MAC</option>
<option value="bulk">{{opt_bulk}}</option>
</select>
<label>{{name}}</label>
<input name="name" autocomplete="off">
<div id="m3u"><label>{{m3u_url}}</label><input name="m3u_url" type="url" placeholder="http://..." autocapitalize="off"></div>
<div id="xtream" hidden>
<label>{{x_url}}</label><input name="x_url" placeholder="http://host:port" autocapitalize="off">
<label>{{x_user}}</label><input name="x_user" autocapitalize="off" autocomplete="off">
<label>{{x_pass}}</label><input name="x_pass" autocapitalize="off" autocomplete="off">
</div>
<div id="stalker" hidden>
<label>{{s_url}}</label><input name="s_url" placeholder="http://host/c/" autocapitalize="off">
<label>{{s_mac}}</label><input name="s_mac" value="00:1A:79:" autocapitalize="characters" autocomplete="off">
<div class="hint">{{hint_mac}}</div>
</div>
<div id="bulk" hidden>
<label>{{bulk_file}}</label><input type="file" accept=".txt,text/plain" onchange="rf(this)">
<label>{{bulk_text}}</label><textarea name="bulk_text" id="bt" rows="8" autocapitalize="off" placeholder="http://host/list.m3u&#10;http://host:8080 user pass&#10;http://portal/c/ 00:1A:79:AA:BB:CC"></textarea>
<div class="hint">{{bulk_hint}}</div>
</div>
<button type="submit">{{send}}</button>
</form>
<script>
function sw(){var v=document.getElementById("t").value;["m3u","xtream","stalker","bulk"].forEach(function(x){document.getElementById(x).hidden=(x!==v)})}
function rf(el){var f=el.files[0];if(!f)return;var r=new FileReader();r.onload=function(){document.getElementById("bt").value=r.result};r.readAsText(f)}
var tp=new URLSearchParams(location.search).get("t");if(tp){document.getElementById("t").value=tp}
sw();
</script>
</body></html>"""

    const val SENT = """<!doctype html>
<html dir="{{dir}}"><head><meta charset="utf-8">
<meta name="viewport" content="width=device-width,initial-scale=1">
<title>Musta IPTV</title>
<style>body{font-family:system-ui,sans-serif;padding:24px;background:#0b0e14;color:#eaf0fa;max-width:520px;margin:auto}</style>
</head><body>
<h1 style="font-size:20px;font-weight:500" id="m">{{sent}}</h1>
<script>
var D="{{done}}";var F="{{failed}}";var P="{{progress}}";var B1="{{bulk_added}}";var B2="{{bulk_failed}}";var pin="{{pin}}";
function poll(){
fetch("/status?k="+pin).then(function(r){return r.text()}).then(function(t){
var m=document.getElementById("m");
if(t.indexOf("done:")==0){m.textContent=D+t.substring(5);return}
if(t.indexOf("failed:")==0){m.textContent=F+t.substring(7);return}
if(t.indexOf("bulk:")==0){var q=t.substring(5).split("/");m.textContent=B1+q[0]+B2+q[1];return}
if(t.indexOf("progress:")==0){m.textContent=P+t.substring(9)}
setTimeout(poll,1500)
}).catch(function(){setTimeout(poll,3000)})
}
poll();
</script>
</body></html>"""

    const val MESSAGE = """<!doctype html>
<html dir="{{dir}}"><head><meta charset="utf-8">
<meta name="viewport" content="width=device-width,initial-scale=1">
<title>Musta IPTV</title>
<style>body{font-family:system-ui,sans-serif;padding:24px;background:#0b0e14;color:#eaf0fa;max-width:520px;margin:auto}a{color:#3d8bff}</style>
</head><body>
<p>{{message}}</p>
{{back_link}}
</body></html>"""

    private val EN = mapOf(
        "title" to "Add a source to Musta IPTV",
        "type" to "Source type",
        "name" to "Name (optional)",
        "m3u_url" to "Playlist URL",
        "x_url" to "Server address",
        "x_user" to "Username",
        "x_pass" to "Password",
        "s_url" to "Portal address",
        "s_mac" to "MAC address",
        "hint_mac" to "Use the MAC address registered with your provider.",
        "send" to "Send to TV",
        "sent" to "Sent to your TV. Loading channels...",
        "done" to "Done. Channels loaded: ",
        "failed" to "Could not add the source: ",
        "bad_link" to "This link is not valid. Open the address shown on your TV again.",
        "e_url" to "The address must start with http:// or https://",
        "e_login" to "Username and password are required.",
        "e_mac" to "The MAC address must look like 00:1A:79:AA:BB:CC",
        "back" to "Go back",
        "opt_bulk" to "Many servers (TXT file)",
        "bulk_file" to "Choose a TXT file (optional)",
        "bulk_text" to "Servers, one per line",
        "bulk_hint" to "Formats: playlist URL | server user pass | portal-URL MAC. Lines starting with # are ignored.",
        "progress" to "Adding sources: ",
        "bulk_added" to "Sources added: ",
        "bulk_failed" to ", failed: ",
        "e_bulk" to "No valid lines were found in the list.",
    )

    private val AR = mapOf(
        "title" to "إضافة مصدر إلى Musta IPTV",
        "type" to "نوع المصدر",
        "name" to "الاسم (اختياري)",
        "m3u_url" to "رابط القائمة",
        "x_url" to "عنوان السيرفر",
        "x_user" to "اسم المستخدم",
        "x_pass" to "كلمة المرور",
        "s_url" to "عنوان البوابة",
        "s_mac" to "عنوان MAC",
        "hint_mac" to "استخدم عنوان MAC المسجّل لدى مزوّدك.",
        "send" to "إرسال إلى التلفاز",
        "sent" to "تم الإرسال إلى التلفاز. جارٍ تحميل القنوات...",
        "done" to "تم. عدد القنوات: ",
        "failed" to "تعذّرت إضافة المصدر: ",
        "bad_link" to "الرابط غير صالح. افتح العنوان الظاهر على التلفاز من جديد.",
        "e_url" to "يجب أن يبدأ العنوان بـ http:// أو https://",
        "e_login" to "اسم المستخدم وكلمة المرور مطلوبان.",
        "e_mac" to "يجب أن يكون MAC بهذا الشكل 00:1A:79:AA:BB:CC",
        "back" to "رجوع",
        "opt_bulk" to "عدة سيرفرات (ملف TXT)",
        "bulk_file" to "اختر ملف TXT (اختياري)",
        "bulk_text" to "السيرفرات، سطر لكل سيرفر",
        "bulk_hint" to "الصيغ: رابط قائمة | سيرفر مستخدم كلمة_المرور | رابط_البوابة MAC. الأسطر التي تبدأ بـ # تُتجاهل.",
        "progress" to "جارٍ إضافة المصادر: ",
        "bulk_added" to "تمت إضافة: ",
        "bulk_failed" to "، فشل: ",
        "e_bulk" to "لم يُعثر على أي سطر صالح في القائمة.",
    )

    private val RU = mapOf(
        "title" to "Добавить источник в Musta IPTV",
        "type" to "Тип источника",
        "name" to "Название (необязательно)",
        "m3u_url" to "Ссылка на плейлист",
        "x_url" to "Адрес сервера",
        "x_user" to "Логин",
        "x_pass" to "Пароль",
        "s_url" to "Адрес портала",
        "s_mac" to "MAC-адрес",
        "hint_mac" to "Используйте MAC-адрес, зарегистрированный у вашего провайдера.",
        "send" to "Отправить на ТВ",
        "sent" to "Отправлено на ТВ. Загружаем каналы...",
        "done" to "Готово. Загружено каналов: ",
        "failed" to "Не удалось добавить источник: ",
        "bad_link" to "Ссылка недействительна. Откройте адрес с экрана ТВ ещё раз.",
        "e_url" to "Адрес должен начинаться с http:// или https://",
        "e_login" to "Нужны логин и пароль.",
        "e_mac" to "MAC-адрес должен выглядеть так: 00:1A:79:AA:BB:CC",
        "back" to "Назад",
        "opt_bulk" to "Много серверов (файл TXT)",
        "bulk_file" to "Выберите файл TXT (необязательно)",
        "bulk_text" to "Серверы, по одному в строке",
        "bulk_hint" to "Форматы: ссылка на плейлист | сервер логин пароль | адрес_портала MAC. Строки с # игнорируются.",
        "progress" to "Добавляем источники: ",
        "bulk_added" to "Добавлено: ",
        "bulk_failed" to ", не удалось: ",
        "e_bulk" to "В списке не найдено подходящих строк.",
    )
}
