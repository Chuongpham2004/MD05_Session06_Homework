package org.example.transactionservice.client;

import org.example.transactionservice.client.dto.AccountDto;
import org.example.transactionservice.client.dto.TransferBalanceRequest;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

/**
 * Feign client gọi ĐỒNG BỘ sang account-service.
 * URL lấy từ cấu hình account-service.url = http://account-service:8080
 * -> "account-service" là tên container, được DNS nội bộ của Docker (banking-network) phân giải.
 */
@FeignClient(name = "account-service", url = "${account-service.url}")
public interface AccountClient {

    @GetMapping("/api/accounts/number/{accountNumber}")
    AccountDto getAccountByNumber(@PathVariable("accountNumber") String accountNumber);

    @PostMapping("/api/accounts/transfer")
    void transferBalance(@RequestBody TransferBalanceRequest request);
}
