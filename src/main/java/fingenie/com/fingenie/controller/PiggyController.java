package fingenie.com.fingenie.controller;

import fingenie.com.fingenie.dto.PiggyBankMemberResponse;
import fingenie.com.fingenie.dto.PiggyBankResponse;
import fingenie.com.fingenie.service.PiggyBankMemberService;
import fingenie.com.fingenie.service.PiggyBankService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("${api-prefix}/piggies")
@RequiredArgsConstructor
public class PiggyController {

    private final PiggyBankService piggyBankService;
    private final PiggyBankMemberService piggyBankMemberService;

    @GetMapping("/{piggyId}")
    public PiggyBankResponse getById(@PathVariable Long piggyId) {
        return piggyBankService.getById(piggyId);
    }

    @GetMapping("/{piggyId}/members")
    public List<PiggyBankMemberResponse> getMembers(@PathVariable Long piggyId) {
        return piggyBankMemberService.getByPiggyBank(piggyId);
    }
}
