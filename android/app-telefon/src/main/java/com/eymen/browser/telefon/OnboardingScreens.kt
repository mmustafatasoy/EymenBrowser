package com.eymen.browser.telefon

import android.provider.Settings
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.eymen.browser.tema.EymenTokens as T
import kotlinx.coroutines.delay

/**
 * Aşama 2 başlangıcı: T01 ve T02 onaylı HTML/PNG düzeninden.
 * GeckoView ayrı kalır: fullscreen, DRM, download ve oturum akışları korunur.
 * Bu iki ekran tüm T01–T11 arayüzünün tamamlandığı anlamına gelmez.
 */
@Composable
internal fun FirstRunOverlay(
    onComplete: (shieldWanted: Boolean, searchEngine: String, addressAtBottom: Boolean, requestDefault: Boolean) -> Unit
) {
    val context = LocalContext.current
    var showSplash by remember { mutableStateOf(true) }
    LaunchedEffect(Unit) {
        // Sistem animasyonları kapalıysa 900 ms açılış bekletilmez.
        val motionScale = Settings.Global.getFloat(
            context.contentResolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f
        )
        if (motionScale > 0f) delay(T.hareket_acilisToplamMs.toLong())
        showSplash = false
    }
    if (showSplash) SplashView() else SetupView(onComplete)
}

@Composable
private fun SplashView() {
    Box(Modifier.fillMaxSize().background(T.gece_zemin)) {
        Canvas(Modifier.fillMaxSize()) {
            val scale = size.width / 390f
            val center = Offset(size.width * 0.5f, size.height * 0.445f)
            listOf(124f, 192f, 268f, 352f).forEach { radius ->
                drawCircle(
                    T.gece_yuzey2, radius = radius * scale, center = center,
                    style = Stroke(width = scale)
                )
            }
        }
        Column(
            Modifier.fillMaxSize().padding(top = 72.dp, bottom = 56.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Spacer(Modifier.height(24.dp))
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                BrandMark(152)
                Spacer(Modifier.height(28.dp))
                LabelText(stringResource(R.string.onboarding_brand), 32, FontWeight.SemiBold)
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                LabelText(stringResource(R.string.onboarding_author), 15, FontWeight.Medium)
                Spacer(Modifier.height(4.dp))
                LabelText(
                    stringResource(R.string.onboarding_channel), 13,
                    color = T.gece_yaziIkincil
                )
            }
        }
    }
}

@Composable
internal fun BrandMark(sizeDp: Int, day: Boolean = false) {
    Canvas(Modifier.size(sizeDp.dp)) {
        val k = size.width / 64f
        drawArc(
            if (day) T.gunduz_vurgu else T.gece_vurgu,
            startAngle = 56f,
            sweepAngle = 248f,
            useCenter = false,
            topLeft = Offset(6f * k, 6f * k),
            size = Size(52f * k, 52f * k),
            style = Stroke(width = 7f * k, cap = StrokeCap.Round)
        )
        listOf(
            Triple(17f, 33f, if (day) T.gunduz_yazi else T.gece_yazi),
            Triple(28.5f, 24f, if (day) T.gunduz_vurgu else T.gece_vurgu),
            Triple(40f, 33f, if (day) T.gunduz_yazi else T.gece_yazi)
        ).forEach { (y, w, c) ->
            drawRoundRect(
                color = c,
                topLeft = Offset(21f * k, y * k),
                size = Size(w * k, 7f * k),
                cornerRadius = CornerRadius(3.5f * k)
            )
        }
    }
}

@Composable
private fun SetupView(
    onComplete: (Boolean, String, Boolean, Boolean) -> Unit
) {
    var shieldWanted by remember { mutableStateOf(true) }
    var searchEngine by remember { mutableStateOf("Google") }
    var addressBottom by remember { mutableStateOf(false) }
    Column(
        Modifier.fillMaxSize()
            .background(T.gece_zemin)
            .padding(start = 20.dp, end = 20.dp, top = 64.dp, bottom = 32.dp)
    ) {
        Column(
            Modifier.weight(1f).verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            LabelText(
                stringResource(R.string.onboarding_headline), 30,
                FontWeight.SemiBold,
                // The approved T02 heading wraps after "sonra" at 390dp.
                // The system font is narrower than Sora; constrain its line width.
                modifier = Modifier.widthIn(max = 300.dp)
            )
            LabelText(
                stringResource(R.string.onboarding_caption), 15,
                color = T.gece_yaziIkincil
            )
            Spacer(Modifier.height(8.dp))

            SetupCard {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    EymenVector(com.eymen.browser.tema.EymenPaths.kalkan, color = T.gece_koruma)
                    Spacer(Modifier.width(12.dp))
                    LabelText(
                        stringResource(R.string.onboarding_shield), 16,
                        FontWeight.SemiBold, modifier = Modifier.weight(1f)
                    )
                    Box(
                        Modifier.size(width = 52.dp, height = 32.dp)
                            .background(
                                if (shieldWanted) T.gece_vurgu else T.gece_yuzey3,
                                RoundedCornerShape(16.dp)
                            )
                            .clickable { shieldWanted = !shieldWanted }
                            .padding(4.dp),
                        contentAlignment = if (shieldWanted) Alignment.CenterEnd else Alignment.CenterStart
                    ) {
                        Box(
                            Modifier.size(24.dp)
                                .background(T.gece_vurguUstu, RoundedCornerShape(12.dp))
                        )
                    }
                }
                LabelText(
                    stringResource(R.string.onboarding_shield_caption), 14,
                    color = T.gece_yaziIkincil
                )
            }

            SetupCard {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    EymenVector(com.eymen.browser.tema.EymenPaths.ara, color = T.gece_vurgu)
                    Spacer(Modifier.width(12.dp))
                    LabelText(
                        stringResource(R.string.onboarding_search_engine), 16,
                        FontWeight.SemiBold
                    )
                }
                listOf(
                    listOf("Google", "DuckDuckGo"),
                    listOf("Bing", "Yandex")
                ).forEach { group ->
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        group.forEach { engine ->
                            Choice(
                                text = engine,
                                selected = searchEngine == engine,
                                modifier = Modifier.weight(1f)
                            ) { searchEngine = engine }
                        }
                    }
                }
            }

            SetupCard {
                LabelText(
                    stringResource(R.string.onboarding_address_position), 16,
                    FontWeight.SemiBold
                )
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    listOf(true, false).forEach { bottom ->
                        val selected = addressBottom == bottom
                        Column(
                            Modifier.weight(1f)
                                .background(
                                    if (selected) T.gece_yuzey3 else T.gece_yuzey1,
                                    RoundedCornerShape(T.telefon_kose_kucuk.dp)
                                )
                                .border(
                                    if (selected) 2.dp else 1.dp,
                                    if (selected) T.gece_vurgu else T.gece_cizgi,
                                    RoundedCornerShape(T.telefon_kose_kucuk.dp)
                                )
                                .clickable { addressBottom = bottom }
                                .padding(10.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Box(
                                Modifier.size(width = 56.dp, height = 76.dp)
                                    .border(
                                        1.dp, T.gece_yaziPasif,
                                        RoundedCornerShape(10.dp)
                                    ).padding(6.dp),
                                contentAlignment =
                                    if (bottom) Alignment.BottomCenter else Alignment.TopCenter
                            ) {
                                Box(
                                    Modifier.fillMaxWidth().height(7.dp)
                                        .background(
                                            T.gece_vurgu,
                                            RoundedCornerShape(4.dp)
                                        )
                                )
                            }
                            Spacer(Modifier.height(8.dp))
                            LabelText(
                                if (bottom) stringResource(R.string.onboarding_bottom)
                                else stringResource(R.string.onboarding_top),
                                15, FontWeight.Medium
                            )
                        }
                    }
                }
            }
        }
        Spacer(Modifier.height(10.dp))
        Box(
            Modifier.fillMaxWidth().height(52.dp)
                .background(T.gece_vurgu, RoundedCornerShape(26.dp))
                .clickable { onComplete(shieldWanted, searchEngine, addressBottom, false) },
            contentAlignment = Alignment.Center
        ) {
            LabelText(
                stringResource(R.string.start), 16, FontWeight.SemiBold,
                color = T.gece_vurguUstu
            )
        }
        Box(
            Modifier.fillMaxWidth().height(44.dp)
                .clickable { onComplete(shieldWanted, searchEngine, addressBottom, true) },
            contentAlignment = Alignment.Center
        ) {
            LabelText(
                stringResource(R.string.onboarding_default_browser), 15,
                FontWeight.Medium
            )
        }
    }
}

@Composable
private fun SetupCard(content: @Composable ColumnScope.() -> Unit) {
    Column(
        Modifier.fillMaxWidth()
            .background(T.gece_yuzey1, RoundedCornerShape(T.telefon_kose_kart.dp))
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        content = content
    )
}

@Composable
private fun Choice(text: String, selected: Boolean, modifier: Modifier, action: () -> Unit) {
    Box(
        modifier.heightIn(min = 48.dp)
            .background(
                if (selected) T.gece_yuzey3 else T.gece_yuzey1,
                RoundedCornerShape(T.telefon_kose_kucuk.dp)
            )
            .border(
                if (selected) 2.dp else 1.dp,
                if (selected) T.gece_vurgu else T.gece_cizgi,
                RoundedCornerShape(T.telefon_kose_kucuk.dp)
            )
            .clickable(onClick = action).padding(8.dp),
        contentAlignment = Alignment.Center
    ) {
        LabelText(
            text, 15, FontWeight.Medium,
            if (selected) T.gece_yazi else T.gece_yaziIkincil,
            align = TextAlign.Center
        )
    }
}

@Composable
private fun LabelText(
    value: String,
    size: Int,
    weight: FontWeight = FontWeight.Normal,
    color: Color = T.gece_yazi,
    modifier: Modifier = Modifier,
    align: TextAlign = TextAlign.Start
) {
    BasicText(
        value,
        modifier = modifier,
        style = TextStyle(
            fontSize = size.sp,
            color = color,
            fontWeight = weight,
            textAlign = align,
            lineHeight = (size * 1.23f).sp
        )
    )
}
