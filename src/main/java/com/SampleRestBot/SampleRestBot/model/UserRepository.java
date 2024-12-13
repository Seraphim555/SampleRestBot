package com.SampleRestBot.SampleRestBot.model;
import org.springframework.data.repository.CrudRepository;

public interface UserRepository extends CrudRepository <User, Long>{
    User findByChatId(Long chatId);
}
