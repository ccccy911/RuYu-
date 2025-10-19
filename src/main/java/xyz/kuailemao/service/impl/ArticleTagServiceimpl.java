package xyz.kuailemao.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;
import xyz.kuailemao.domain.entity.ArticleTag;
import xyz.kuailemao.mapper.ArticleTagMapper;
import xyz.kuailemao.service.ArticleTagService;
@Service
public class ArticleTagServiceimpl extends ServiceImpl<ArticleTagMapper, ArticleTag> implements ArticleTagService {
}
