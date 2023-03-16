package com.example.myapplicationwithuniquesdk

import android.os.Bundle
import android.os.StrictMode
import android.os.StrictMode.ThreadPolicy
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.example.myapplicationwithuniquesdk.databinding.FragmentSecondBinding
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import network.unique.model.*
import network.unique.sdk.UniqueSdk
import network.unique.sdk.android.PasswordStorageHelper
import network.unique.service.BalanceService
import java.math.BigDecimal
import java.nio.charset.StandardCharsets


/**
 * A simple [Fragment] subclass as the second destination in the navigation.
 */
class SecondFragment : Fragment() {

    private var _binding: FragmentSecondBinding? = null

    private lateinit var storageHelper: PasswordStorageHelper

    // This property is only valid between onCreateView and
    // onDestroyView.
    private val binding get() = _binding!!

    protected val scope = CoroutineScope(Dispatchers.Main)

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {

        _binding = FragmentSecondBinding.inflate(inflater, container, false)
        return binding.root

    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val policy = ThreadPolicy.Builder().permitAll().build()

        StrictMode.setThreadPolicy(policy)

        // Создаём хранилище
        storageHelper = PasswordStorageHelper(requireActivity())

        // Кладём значение в хранилище
        storageHelper.setData(
            "seed",
            "charge fame control elephant taxi among brain latin meadow oven crash another".toByteArray()
        )

        // Получаем значение из хранилища
        val seed = storageHelper.getData("seed")!!.toString(StandardCharsets.UTF_8)

        // Создаём объект для подписи транзакции Sr25519SignerWrapper(seed, password)
        val signer = Sr25519SignerWrapper(seed, null, false)
        // Создаём SDK для вызова API
        val sdk = UniqueSdk("https://rest.unique.network/opal")

        UniqueSdk.signerWrapper = signer

        scope.launch {
            // Сервисы созданы согласно документации свагера
            val balanceService: BalanceService = sdk.balanceService;

            // Если метод сервиса является мутацией, то в сервисе будет вложенный сервис для работы с методом
            val transferMutationService = balanceService.getTransfer();
            val transferBody = TransferMutationRequest(
                "5DnUE1uV7iW25bUriWVPHY67KMm2t6g5v23GzVbZCUc8fyBD",
                "unjKJQJrRd238pkUZZvzDQrfKuM39zBSnQ5zjAGAGcdRhaJTx",
                BigDecimal("0.01")
            )

            // Билдим транзакцию
            val transferResponse = transferMutationService.build(transferBody)

            val signBody = UnsignedTxPayloadResponse(
                transferResponse.signerPayloadJSON,
                transferResponse.signerPayloadRaw,
                transferResponse.signerPayloadHex
            )

            // Локально подписываем
            val signResponse = transferMutationService.sign(signBody)

            val submitBody = SubmitTxBody(signResponse.signerPayloadJSON, signResponse.signature)

            // Подтверждаем
            val submitResponse = transferMutationService.submitWatch(submitBody)

            binding.textviewSecond.text = submitResponse.hash
        }

        scope.launch {
            val collection = sdk.collectionService
            val createCollection = collection.getCreateCollection()

            val submitResponse = createCollection.submitWatch(
                CreateCollectionMutationRequest(
                    address =  "5DnUE1uV7iW25bUriWVPHY67KMm2t6g5v23GzVbZCUc8fyBD",
                    name = "Unique SDK Collection Android Test 2",
                    description = "Unique SDK Collection Android Test",
                    tokenPrefix = "MAD2"
                )
            )

            val hash = submitResponse.hash!!

//            val extrinsicStatus = sdk.extrinsicService.getExtrinsicStatus(hash)

            binding.textViewThird.text = hash
        }

        scope.launch {
            val token = sdk.tokenService
            val createToken = token.getCreateToken()

            val submitResponse = createToken.submitWatch(
                CreateNewTokenMutationRequest(
                    address =  "5DnUE1uV7iW25bUriWVPHY67KMm2t6g5v23GzVbZCUc8fyBD",
                    collectionId = BigDecimal.valueOf(898),
                    data = UniqueTokenToCreateDto(
                        image = UniqueTokenToCreateDtoImage(
                            ipfsCid = "QmPh459X5FMnUCzVnbm9he7YmiehhUpfHRfyXbcN53ZW4Y"
                        )
                    )
                )
            )

            val hash = submitResponse.hash!!

//            val extrinsicStatus = sdk.extrinsicService.getExtrinsicStatus(hash)

            binding.textViewFourth.text = hash
        }

        binding.buttonSecond.setOnClickListener {
            findNavController().navigate(R.id.action_SecondFragment_to_FirstFragment)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
