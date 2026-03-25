package com.therouter.app.compose.brick

import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.fragment.app.FragmentActivity
import com.therouter.app.compose.PathIndex.BRICK
import com.therouter.app.compose.PathIndex.TEST_COMPOSE1
import com.therouter.app.compose.PathIndex.TEST_COMPOSE2
import com.therouter.app.compose.PathIndex.TEST_COMPOSE3
import com.therouter.app.compose.PathIndex.TEST_COMPOSE4
import com.therouter.app.compose.PathIndex.TEST_COMPOSE5
import com.therouter.app.compose.PathIndex.TEST_COMPOSE6
import com.therouter.app.compose.PathIndex.TEST_COMPOSE7
import com.therouter.app.compose.PathIndex.TEST_COMPOSE8
import com.therouter.app.compose.PathIndex.TEST_COMPOSE9
import com.therouter.app.compose.PathIndex.TEXT_COMPOSE
import com.therouter.TheRouter
import com.therouter.brick.annotation.DataProvider
import com.therouter.compose.ComposeViewFactory
import com.therouter.app.R
import com.therouter.compose.compose
import com.therouter.router.Navigator
import com.therouter.router.Route

@Route(path = BRICK)
class BrickActivity : FragmentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        setContentView(R.layout.main_activity)

        val view = ComposeViewFactory.makeView(this) {
            BrickPage()
        }
        setContentView(view)
    }
}

@Composable
fun BrickPage() {
    LazyColumn(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        ArrayList<String>().apply {
            add(TEST_COMPOSE1)
            add(TEST_COMPOSE2)
            add(TEST_COMPOSE4)
            add(TEST_COMPOSE5)
            add(TEST_COMPOSE6)
            add(TEST_COMPOSE7)
            add(TEST_COMPOSE8)
            add(TEST_COMPOSE9)
        }.forEach {
            item { TheRouter.build(it).compose() }
        }
    }
}

@DataProvider(path = TEXT_COMPOSE)
fun make(navigator: Navigator): String {
    return navigator.getUrlWithParams()
}

@Route(path = TEXT_COMPOSE)
@Composable
fun ui(str: String) {
    Text(
        modifier = Modifier.padding(start = 16.dp, top = 16.dp, end = 16.dp),
        text = str,
        style = MaterialTheme.typography.titleMedium,
    )
}

@Route(path = TEST_COMPOSE3)
@Composable
fun ServiceCard(title: String, subtitle: String) {
    Card(
        modifier = Modifier.padding(1.dp),
        shape = RoundedCornerShape(12.dp),
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(text = title, style = MaterialTheme.typography.bodyMedium)
            Text(text = subtitle, style = MaterialTheme.typography.bodySmall)
        }
    }
}

@Route(path = TEST_COMPOSE4)
@Composable
fun AdSection() {
    val ads = listOf("265元搬家券包", "新人礼包", "限时活动")
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp)
            .background(
                color = MaterialTheme.colorScheme.surfaceVariant,
                shape = RoundedCornerShape(12.dp),
            )
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(text = "广告", style = MaterialTheme.typography.titleMedium)
        LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            items(ads) { title ->
                Card(shape = RoundedCornerShape(12.dp)) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(text = title, style = MaterialTheme.typography.bodyMedium)
                        Text(text = "主视觉/优惠说明", style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
        }
    }
}

@Route(path = TEST_COMPOSE5)
@Composable
fun ServiceIntroSection() {
    val items = listOf("专业搬运", "拆装打包", "价格透明", "售后保障")
    val selectedIndex = remember { mutableStateOf(0) }
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp)
            .background(
                color = MaterialTheme.colorScheme.surfaceVariant,
                shape = RoundedCornerShape(12.dp),
            )
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(text = "服务介绍", style = MaterialTheme.typography.titleMedium)
        Card(shape = RoundedCornerShape(12.dp)) {
            Column(
                modifier = Modifier.padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Text(
                    text = "主图：${items[selectedIndex.value]}",
                    style = MaterialTheme.typography.bodyMedium,
                )
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    items.forEachIndexed { index, title ->
                        Text(
                            text = title,
                            modifier = Modifier
                                .clickable { selectedIndex.value = index }
                                .padding(vertical = 4.dp, horizontal = 8.dp),
                            style = if (index == selectedIndex.value) {
                                MaterialTheme.typography.bodyMedium
                            } else {
                                MaterialTheme.typography.bodySmall
                            },
                        )
                    }
                }
            }
        }
    }
}

@Route(path = TEST_COMPOSE6)
@Composable
fun PricingSection() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp)
            .background(
                color = MaterialTheme.colorScheme.surfaceVariant,
                shape = RoundedCornerShape(12.dp),
            )
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(text = "费用标准", style = MaterialTheme.typography.titleMedium)
        Card(shape = RoundedCornerShape(12.dp)) {
            Column(
                modifier = Modifier.padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text(text = "基础服务费 + 额外服务费", style = MaterialTheme.typography.bodyMedium)
                Text(text = "查看全部费用 >", style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}

@Route(path = TEST_COMPOSE7)
@Composable
fun UserReviewsSection() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp)
            .background(
                color = MaterialTheme.colorScheme.surfaceVariant,
                shape = RoundedCornerShape(12.dp),
            )
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(text = "用户评论", style = MaterialTheme.typography.titleMedium)
        Card(shape = RoundedCornerShape(12.dp)) {
            Column(
                modifier = Modifier.padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text(text = "阿包 ★★★★★", style = MaterialTheme.typography.bodyMedium)
                Text(text = "师傅准时又专业，打包仔细。", style = MaterialTheme.typography.bodySmall)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    repeat(3) {
                        Card(
                            modifier = Modifier.size(64.dp),
                            shape = RoundedCornerShape(8.dp),
                        ) {}
                    }
                }
                Text(text = "查看全部评价", style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}

@Route(path = TEST_COMPOSE8)
@Composable
fun FaqSection() {
    val faqs = listOf(
        "可以跟车几个人？" to "需遵循安全与座位规定，具体以司机确认。",
        "什么车型合适？" to "根据物品体积和楼层情况选择车型。",
        "搬家怎么收费？" to "基础服务费 + 额外服务费，按需计费。",
    )
    val expanded = remember { mutableStateOf(List(faqs.size) { false }) }
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp)
            .background(
                color = MaterialTheme.colorScheme.surfaceVariant,
                shape = RoundedCornerShape(12.dp),
            )
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(text = "常见问题", style = MaterialTheme.typography.titleMedium)
        Card(shape = RoundedCornerShape(12.dp)) {
            Column(
                modifier = Modifier.padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                faqs.forEachIndexed { index, item ->
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                val next = expanded.value.toMutableList()
                                next[index] = !next[index]
                                expanded.value = next
                            }
                            .padding(vertical = 4.dp),
                    ) {
                        Text(text = item.first, style = MaterialTheme.typography.bodyMedium)
                        if (expanded.value[index]) {
                            Text(text = item.second, style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
            }
        }
    }
}

@Route(path = TEST_COMPOSE9)
@Composable
fun BrandHonorsSection() {
    val honors = listOf("行业认可", "媒体报道", "用户口碑")
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp)
            .background(
                color = MaterialTheme.colorScheme.surfaceVariant,
                shape = RoundedCornerShape(12.dp),
            )
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(text = "品牌荣誉", style = MaterialTheme.typography.titleMedium)
        LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            items(honors) { title ->
                Card(shape = RoundedCornerShape(12.dp)) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(text = title, style = MaterialTheme.typography.bodyMedium)
                        Text(text = "荣誉展示图", style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
        }
    }
}
