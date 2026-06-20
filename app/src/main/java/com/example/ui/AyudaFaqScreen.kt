package com.example.ui

import androidx.compose.animation.*
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun AyudaFaqScreen() {
    val faqItems = listOf(
        FaqData(
            id = 1,
            pregunta = "¿Venenosa vs Peligrosa?",
            respuesta = "Casi TODAS las arañas chilenas son venenosas ya que usan el veneno para paralizar a sus presas. Sin embargo, muy pocas tienen un veneno PELIGROSO capaz de dañar gravemente la salud o tejidos de los humanos.\n\nEn Chile, solo la ARAÑA DE RINCÓN (Loxosceles laeta) y la ARAÑA DE TRIGO / Viuda Negra (Latrodectus mactans) se consideran de importancia médica extrema (altamente peligrosas). Las demás son aliadas pacíficas."
        ),
        FaqData(
            id = 2,
            pregunta = "¿Cuál es el rol de las arañas en el ecosistema?",
            respuesta = "Las arañas son controladores biológicos vitales. Se alimentan de moscas, mosquitos, polillas, zancudos y, crucialmente, la Araña Tigre (Scytodes globula) se alimenta de la temida Araña de Rincón.\n\nEliminar indiscriminadamente arañas silvestres provoca un aumento descontrolado de plagas indeseadas en nuestros hogares y campos."
        ),
        FaqData(
            id = 3,
            pregunta = "¿Qué hacer si me muerde una araña de rincón?",
            respuesta = "1. Mantén la calma y lava la zona afectada con agua y jabón.\n2. Aplica hielo local (el frío frena la acción necrótica del veneno).\n3. Captura la araña (viva o muerta) para facilitar su identificación médica.\n4. Dirígete INMEDIATAMENTE al servicio de urgencias más cercano (Hospital o Posta)."
        ),
        FaqData(
            id = 4,
            pregunta = "¿Cómo reconozco a la Araña Tigre chilena?",
            respuesta = "La Araña Tigre (Scytodes globula) tiene patas extremadamente largas, delgadas e hiladas con un diseño amarillo con negro muy característico. Se mueve lentamente y caza de forma nocturna escupiendo una red adhesiva sobre sus presas. Es inofensiva para humanos y una gran aliada."
        )
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text(
            text = "¿Qué quieres aprender hoy?",
            color = MaterialTheme.colorScheme.onBackground,
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 4.dp)
        )
        Text(
            text = "Herramientas de educación y prevención para la comunidad de Chile.",
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f),
            style = MaterialTheme.typography.bodySmall,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier.weight(1f)
        ) {
            items(faqItems) { faq ->
                FaqExpandableCard(faq = faq)
            }
        }
    }
}

data class FaqData(val id: Int, val pregunta: String, val respuesta: String)

@Composable
fun FaqExpandableCard(faq: FaqData) {
    var expanded by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { expanded = !expanded }
            .testTag("faq_card_${faq.id}"),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = faq.pregunta,
                    color = MaterialTheme.colorScheme.onSurface,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f)
                )
                Icon(
                    imageVector = if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
            }

            AnimatedVisibility(
                visible = expanded,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut()
            ) {
                Column {
                    Spacer(modifier = Modifier.height(10.dp))
                    HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f))
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = faq.respuesta,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f),
                        style = MaterialTheme.typography.bodySmall,
                        lineHeight = 18.sp
                    )
                }
            }
        }
    }
}
