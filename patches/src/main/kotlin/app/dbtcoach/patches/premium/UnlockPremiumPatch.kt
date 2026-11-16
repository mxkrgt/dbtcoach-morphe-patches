package app.dbtcoach.patches.premium

import app.dbtcoach.patches.shared.Constants.PACKAGE_DBTCOACH
import app.dbtcoach.patches.shared.Constants.VERSIONS_DBTCOACH
import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patcher.util.proxy.mutableTypes.MutableMethod.Companion.toMutable
import com.android.tools.smali.dexlib2.AccessFlags
import com.android.tools.smali.dexlib2.builder.MutableMethodImplementation
import com.android.tools.smali.dexlib2.immutable.ImmutableMethod
import com.android.tools.smali.dexlib2.immutable.ImmutableMethodParameter

/**
 * Patch : Unlock Premium
 *
 * Verified flow (see analysis/dbt-coach/notes/premium-subscriptions.md) :
 *
 *   initializePremiumContent()
 *     -> fetchPremiumStatus()            // GraphQL getSubscriptionStatus, network-only
 *     -> si hasAccess !== true :
 *          getPurchases()                // fallback local Play Store
 *            -> RNIap.getAvailablePurchases()
 *               -> Nitro hybrid "RnIap"
 *                  -> HybridRnIap.getAvailablePurchases(options)
 *          si purchases.length > 0 -> state.iap.isSubscribed = true (+ HAS_PREMIUM)
 *
 * On branche donc la lecture des achats natifs pour qu'elle renvoie une liste
 * contenant un faux abonnement Android. Le bundle JS considère un achat sans
 * `expirationDateIOS` comme actif, ce qui déverrouille tous les gardes
 * `props.isSubscribed`.
 *
 * Pourquoi une méthode d'aide plutôt qu'un remplacement direct ?
 *   Le corps d'origine utilise 5 registres. Construire le NitroPurchase demande
 *   45 mots de registres ; on ajoute donc une méthode statique dédiée avec son
 *   propre compteur de registres, et la méthode d'origine ne fait plus que
 *   l'appeler et retourner son résultat.
 *
 * Ce patch ne touche ni les achats réels, ni la restauration, ni RevenueCat.
 */

private const val HELPER_NAME = "morphe_unlockPremium_getAvailablePurchases"
private const val HELPER_DESCRIPTOR =
    "$NITRO_IAP_CLASS->$HELPER_NAME($OPTIONS_TYPE)$PROMISE_TYPE"

@Suppress("unused")
val unlockPremiumPatch = bytecodePatch(
    name = "Unlock premium",
    description = "Déverrouille toutes les fonctionnalités premium de DBT Coach en simulant " +
        "un abonnement Google Play actif lors de la lecture des achats natifs.",
) {
    compatibleWith(PACKAGE_DBTCOACH to VERSIONS_DBTCOACH)

    execute {
        val target = GetAvailablePurchasesFingerprint.method
        val classDef = GetAvailablePurchasesFingerprint.classDef

        // 47 registres : 45 mots pour `new NitroPurchase(...)` (2 doubles = 2 mots
        // chacun), plus les registres de travail.
        val helper = ImmutableMethod(
            NITRO_IAP_CLASS,
            HELPER_NAME,
            listOf(ImmutableMethodParameter(OPTIONS_TYPE, null, null)),
            PROMISE_TYPE,
            AccessFlags.PUBLIC.value or AccessFlags.STATIC.value,
            null,
            null,
            MutableMethodImplementation(47),
        ).toMutable().apply {
            addInstructions(0, FAKE_PURCHASE_SMALI)
        }

        classDef.methods.add(helper)

        // La méthode d'origine ne fait plus que déléguer à l'aide puis retourner.
        // Le corps d'origine reste après le `return-object` (jamais exécuté).
        target.addInstructions(
            0,
            """
                invoke-static { p1 }, $HELPER_DESCRIPTOR
                move-result-object p0
                return-object p0
            """,
        )
    }
}

/**
 * Construit `Promise.Companion.resolved(listOf(fakeNitroPurchase))`.
 *
 * Disposition des arguments du constructeur, copiée du smali
 * (classes4/com/margelo/nitro/iap/NitroPurchase.smali:287) :
 *
 *   v0        receiver
 *   v1        id                (non nul)
 *   v2        productId         (non nul)
 *   v3:v4     transactionDate   (double)
 *   v5        purchaseToken     (nullable)
 *   v6        platform          (non nul)
 *   v7        store             (non nul)
 *   v8:v9     quantity          (double)
 *   v10       purchaseState     (non nul)
 *   v11       isAutoRenewing
 *   v12..v44  champs nullables restants (null / 0)
 *
 * Côté JS, `validateNitroPurchase` exige `id`, `productId`, `transactionDate`
 * et `platform` non nuls ; `expirationDateIOS` reste null, ce qui suffit au
 * embranchement Android de `getPurchases()` pour considérer l'achat actif.
 */
private const val FAKE_PURCHASE_SMALI = """
    const-string v1, "dbtcoach.monthly"
    const-string v2, "dbtcoach.monthly"

    invoke-static {}, Ljava/lang/System;->currentTimeMillis()J
    move-result-wide v3
    long-to-double v3, v3

    const/4 v5, 0x0
    sget-object v6, Lcom/margelo/nitro/iap/IapPlatform;->ANDROID:Lcom/margelo/nitro/iap/IapPlatform;
    sget-object v7, Lcom/margelo/nitro/iap/IapStore;->GOOGLE:Lcom/margelo/nitro/iap/IapStore;
    const-wide v8, 0x3ff0000000000000L
    sget-object v10, Lcom/margelo/nitro/iap/PurchaseState;->PURCHASED:Lcom/margelo/nitro/iap/PurchaseState;
    const/4 v11, 0x1

    const/4 v12, 0x0
    const/4 v13, 0x0
    const/4 v14, 0x0
    const/4 v15, 0x0
    const/16 v16, 0x0
    const/16 v17, 0x0
    const/16 v18, 0x0
    const/16 v19, 0x0
    const/16 v20, 0x0
    const/16 v21, 0x0
    const/16 v22, 0x0
    const/16 v23, 0x0
    const/16 v24, 0x0
    const/16 v25, 0x0
    const/16 v26, 0x0
    const/16 v27, 0x0
    const/16 v28, 0x0
    const/16 v29, 0x0
    const/16 v30, 0x0
    const/16 v31, 0x0
    const/16 v32, 0x0
    const/16 v33, 0x0
    const/16 v34, 0x0
    const/16 v35, 0x0
    const/16 v36, 0x0
    const/16 v37, 0x0
    const/16 v38, 0x0
    const/16 v39, 0x0
    const/16 v40, 0x0
    const/16 v41, 0x0
    const/16 v42, 0x0
    const/16 v43, 0x0
    const/16 v44, 0x0

    new-instance v0, Lcom/margelo/nitro/iap/NitroPurchase;

    invoke-direct/range {v0 .. v44}, Lcom/margelo/nitro/iap/NitroPurchase;-><init>(Ljava/lang/String;Ljava/lang/String;DLjava/lang/String;Lcom/margelo/nitro/iap/IapPlatform;Lcom/margelo/nitro/iap/IapStore;DLcom/margelo/nitro/iap/PurchaseState;ZLjava/lang/Double;Ljava/lang/Double;Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;Ljava/lang/Double;Ljava/lang/Boolean;Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;Ljava/lang/Double;Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;Lcom/margelo/nitro/iap/NitroRenewalInfoIOS;Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;Ljava/lang/Boolean;Ljava/lang/Double;Ljava/lang/Boolean;Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;Ljava/lang/Boolean;)V

    const/4 v1, 0x1
    new-array v1, v1, [$PURCHASE_TYPE
    const/4 v2, 0x0
    aput-object v0, v1, v2

    sget-object v3, Lcom/margelo/nitro/core/Promise;->Companion:Lcom/margelo/nitro/core/Promise${'$'}Companion;
    invoke-virtual {v3, v1}, Lcom/margelo/nitro/core/Promise${'$'}Companion;->resolved(Ljava/lang/Object;)Lcom/margelo/nitro/core/Promise;
    move-result-object v0

    return-object v0
"""
