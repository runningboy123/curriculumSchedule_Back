package com.xjtu.service.impl;

import com.xjtu.dao.ClassInfoByClassDao;
import com.xjtu.dao.ClassInfoByCourseDao;
import com.xjtu.dao.ListInfoDao;
import com.xjtu.entity.ClassInfoByClass;
import com.xjtu.entity.ClassInfoByCourse;
import com.xjtu.entity.ListInfo;
import com.xjtu.exception.VerificationException;
import com.xjtu.service.ClassInfoService;
import com.xjtu.service.HtmlParseJsonService;
import com.xjtu.service.UrlDataService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by llh.xjtu on 17-4-24.
 */
@Service
public class ClassInfoServiceImpl implements ClassInfoService {

    private Logger logger = LoggerFactory.getLogger(ClassInfoService.class);

    @Autowired
    private ClassInfoByCourseDao classInfoByCourseDao;
    
    @Autowired
    private UrlDataService urlDataService;

    @Autowired
    private HtmlParseJsonService htmlParseJsonService;

    @Autowired
    private ListInfoDao listInfoDao;

    @Autowired
    private ListInfo listInfo;




    @Override
    public String code() {
        urlDataService.getCookie();
        urlDataService.getImage(1);
        return urlDataService.getSessionId()+".jpg";
    }

    //
    @Override
    @Transactional
    public Map<String, String> queryList(String term, int type) {
        Map<String, String> map = new HashMap<>();
        List<ListInfo> lists = new ArrayList<>();
        lists = listInfoDao.queryAllList(term, type);
        if (lists.size() == 0) {
            urlDataService.getCookie();
           // urlData.getImage(1,);
            //本地list为空，访问网络
            //1得验证码 ，2得到list
            String string = urlDataService.getXNXQKC(term, "");
            map = htmlParseJsonService.optiontoList(string);
            //存进数据库,速度较慢，后期使用非关系数据库

            for (String s : map.keySet()) {
                listInfo.setTerm(term);
                listInfo.setListContent(map.get(s));
                listInfo.setValue(s);
                listInfo.setType(type);
                listInfoDao.insertCourseList(listInfo);
            }
        } else {
            for (ListInfo listIn : lists) {
                map.put(listIn.getListContent(), listIn.getValue());
            }
        }


        return map;
    }


    @Override
    @Transactional
    public List<ClassInfoByCourse> queryByCourse(String term, String course, String yzm) {
        List<ClassInfoByCourse> lists = new ArrayList<>();
        //查询本地数据库看是否有数据
        lists = classInfoByCourseDao.queryByKeyWithCourse(term, course);
        logger.info("term---course:",term+"---"+course);
        if (lists.size() != 0) {
            //本地数据有数据直接返回数据
            return lists;
        } else {
            //本地数据库无数据查找网络端，并返回
            String type = "1";


            try {
                String string = urlDataService.getKBFBLessonSel(term, course, type, yzm);
                lists = htmlParseJsonService.getClassInfo2(string);
                //往数据库存
                for (ClassInfoByCourse infoByCourse : lists) {
                    infoByCourse.setTerm(term);
                    infoByCourse.setCourse(course);
                    classInfoByCourseDao.insertCourse(infoByCourse);
                }
            } catch (VerificationException e1) {
                logger.error("VerificationException");
                throw new VerificationException("验证码错误");

            }


        }
        return lists;

    }

	
}
