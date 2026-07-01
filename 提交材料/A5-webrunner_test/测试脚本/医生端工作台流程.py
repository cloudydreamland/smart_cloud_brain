# -*- coding: utf-8 -*-
from loguru import logger
from webrunnercore import wr #wr模块是webrunner内置的脚本增强sdk
from webrunnercore import *



class 测试负载(WebLoadMachine):
    '''
    用户自定义负载信息
    '''
    负载名称 = '默认负载'

    测试机 = [
        {
            "ip地址": '172.22.69.149',
            "端口": 50000,
            "节点数": 1,
            "主节点": True
        },
    ]

class 测试场景(WebScenario):
    '''
    用户自定义场景信息
    '''
    场景名称 = '默认场景'

    模式 = '梯形负载'
    参数 = {
        '用户数': 10,
        '创建速率': 3,
        '运行时长': 180
    }



class Transaction_Morenfenzu(SerialTransaction):
    '''
    事务定义, 一个事务由多个task构成, 每个task只包含一个请求
    '''

    def __init__(self, parent: "User") -> None:
        super().__init__(parent)

    @property
    def transaction(self):
        # 事务名称
        return "morenfenzu"

    def on_start(self):
        # 事务启动函数
        super().on_start()
    
    @task
    def task_0(self):
        url = "http://localhost:5174/api/doctor/login"
        headers = {'Accept': '*/*', 'Accept-Language': 'zh-CN,zh;q=0.9', 'Connection': 'keep-alive', 'Content-Length': '45', 'Host': 'localhost:5174', 'Origin': 'http://localhost:5174', 'Referer': 'http://localhost:5174/login?redirect=/', 'Sec-Fetch-Dest': 'empty', 'Sec-Fetch-Mode': 'cors', 'Sec-Fetch-Site': 'same-origin', 'User-Agent': 'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/114.0.0.0 Safari/537.36', 'content-type': 'application/json', 'sec-ch-ua': '"Not.A/Brand";v="8", "Chromium";v="114"', 'sec-ch-ua-mobile': '?0', 'sec-ch-ua-platform': '"Windows"'}
        params = {}
        # application/json (dict)
        data = {}
        data['account'] = '13900000002'
        data['password'] = '123456'

        res = self.post(url, headers=headers, params=params, json=data)
        # application/json,res.json()

    
    @task
    def task_1(self):
        url = "http://localhost:5174/api/registration/list"
        headers = {'Accept': '*/*', 'Accept-Language': 'zh-CN,zh;q=0.9', 'Connection': 'keep-alive', 'Host': 'localhost:5174', 'Referer': 'http://localhost:5174/', 'Sec-Fetch-Dest': 'empty', 'Sec-Fetch-Mode': 'cors', 'Sec-Fetch-Site': 'same-origin', 'User-Agent': 'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/114.0.0.0 Safari/537.36', 'authorization': 'Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiIyIiwicm9sZSI6IkRPQ1RPUiIsIm5hbWUiOiLmnY7ljLvnlJ8iLCJleHAiOjE3ODI5NTcwOTZ9.5VImYYrVcVMHzzK0gkNrueVOx2IG43rBeoCOCUu0YVc', 'sec-ch-ua': '"Not.A/Brand";v="8", "Chromium";v="114"', 'sec-ch-ua-mobile': '?0', 'sec-ch-ua-platform': '"Windows"'}
        params = {}
        # null
        data = None
        res = self.get(url, headers=headers, params=params, data=data)
        # application/json,res.json()

    
    @task
    def task_2(self):
        url = "http://localhost:5174/api/triage/list"
        headers = {'Accept': '*/*', 'Accept-Language': 'zh-CN,zh;q=0.9', 'Connection': 'keep-alive', 'Host': 'localhost:5174', 'Referer': 'http://localhost:5174/', 'Sec-Fetch-Dest': 'empty', 'Sec-Fetch-Mode': 'cors', 'Sec-Fetch-Site': 'same-origin', 'User-Agent': 'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/114.0.0.0 Safari/537.36', 'authorization': 'Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiIyIiwicm9sZSI6IkRPQ1RPUiIsIm5hbWUiOiLmnY7ljLvnlJ8iLCJleHAiOjE3ODI5NTcwOTZ9.5VImYYrVcVMHzzK0gkNrueVOx2IG43rBeoCOCUu0YVc', 'sec-ch-ua': '"Not.A/Brand";v="8", "Chromium";v="114"', 'sec-ch-ua-mobile': '?0', 'sec-ch-ua-platform': '"Windows"'}
        params = {}
        # null
        data = None
        res = self.get(url, headers=headers, params=params, data=data)
        # application/json,res.json()

    
    @task
    def task_3(self):
        url = "http://localhost:5174/api/medical-record/list"
        headers = {'Accept': '*/*', 'Accept-Language': 'zh-CN,zh;q=0.9', 'Connection': 'keep-alive', 'Host': 'localhost:5174', 'Referer': 'http://localhost:5174/', 'Sec-Fetch-Dest': 'empty', 'Sec-Fetch-Mode': 'cors', 'Sec-Fetch-Site': 'same-origin', 'User-Agent': 'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/114.0.0.0 Safari/537.36', 'authorization': 'Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiIyIiwicm9sZSI6IkRPQ1RPUiIsIm5hbWUiOiLmnY7ljLvnlJ8iLCJleHAiOjE3ODI5NTcwOTZ9.5VImYYrVcVMHzzK0gkNrueVOx2IG43rBeoCOCUu0YVc', 'sec-ch-ua': '"Not.A/Brand";v="8", "Chromium";v="114"', 'sec-ch-ua-mobile': '?0', 'sec-ch-ua-platform': '"Windows"'}
        params = {}
        # null
        data = None
        res = self.get(url, headers=headers, params=params, data=data)
        # application/json,res.json()

    
    @task
    def task_4(self):
        url = "http://localhost:5174/api/prescription/list"
        headers = {'Accept': '*/*', 'Accept-Language': 'zh-CN,zh;q=0.9', 'Connection': 'keep-alive', 'Host': 'localhost:5174', 'Referer': 'http://localhost:5174/', 'Sec-Fetch-Dest': 'empty', 'Sec-Fetch-Mode': 'cors', 'Sec-Fetch-Site': 'same-origin', 'User-Agent': 'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/114.0.0.0 Safari/537.36', 'authorization': 'Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiIyIiwicm9sZSI6IkRPQ1RPUiIsIm5hbWUiOiLmnY7ljLvnlJ8iLCJleHAiOjE3ODI5NTcwOTZ9.5VImYYrVcVMHzzK0gkNrueVOx2IG43rBeoCOCUu0YVc', 'sec-ch-ua': '"Not.A/Brand";v="8", "Chromium";v="114"', 'sec-ch-ua-mobile': '?0', 'sec-ch-ua-platform': '"Windows"'}
        params = {}
        # null
        data = None
        res = self.get(url, headers=headers, params=params, data=data)
        # application/json,res.json()

    
    @task
    def task_5(self):
        url = "http://localhost:5174/api/notification/list"
        headers = {'Accept': '*/*', 'Accept-Language': 'zh-CN,zh;q=0.9', 'Connection': 'keep-alive', 'Host': 'localhost:5174', 'Referer': 'http://localhost:5174/', 'Sec-Fetch-Dest': 'empty', 'Sec-Fetch-Mode': 'cors', 'Sec-Fetch-Site': 'same-origin', 'User-Agent': 'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/114.0.0.0 Safari/537.36', 'authorization': 'Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiIyIiwicm9sZSI6IkRPQ1RPUiIsIm5hbWUiOiLmnY7ljLvnlJ8iLCJleHAiOjE3ODI5NTcwOTZ9.5VImYYrVcVMHzzK0gkNrueVOx2IG43rBeoCOCUu0YVc', 'sec-ch-ua': '"Not.A/Brand";v="8", "Chromium";v="114"', 'sec-ch-ua-mobile': '?0', 'sec-ch-ua-platform': '"Windows"'}
        params = {}
        # null
        data = None
        res = self.get(url, headers=headers, params=params, data=data)
        # application/json,res.json()

    
    @task
    def task_6(self):
        url = "http://localhost:5174/api/triage/list"
        headers = {'Accept': '*/*', 'Accept-Language': 'zh-CN,zh;q=0.9', 'Connection': 'keep-alive', 'Host': 'localhost:5174', 'Referer': 'http://localhost:5174/queue', 'Sec-Fetch-Dest': 'empty', 'Sec-Fetch-Mode': 'cors', 'Sec-Fetch-Site': 'same-origin', 'User-Agent': 'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/114.0.0.0 Safari/537.36', 'authorization': 'Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiIyIiwicm9sZSI6IkRPQ1RPUiIsIm5hbWUiOiLmnY7ljLvnlJ8iLCJleHAiOjE3ODI5NTcwOTZ9.5VImYYrVcVMHzzK0gkNrueVOx2IG43rBeoCOCUu0YVc', 'sec-ch-ua': '"Not.A/Brand";v="8", "Chromium";v="114"', 'sec-ch-ua-mobile': '?0', 'sec-ch-ua-platform': '"Windows"'}
        params = {}
        # null
        data = None
        res = self.get(url, headers=headers, params=params, data=data)
        # application/json,res.json()


    def on_stop(self):
        # 事务结束函数
        super().on_stop()


class WebrunnerAction(BrowserAction):
    '''
    事务集合, 一个Action包含多个Transaction事务, @task装饰器参数表示事务混合比，
    初始化事务 和 结束事务只执行一次， 执行事务按照混合比执行多次
    '''
    def __init__(self, parent: "User") -> None:
        super().__init__(parent)

    def on_start(self):
        super().on_start()

    @task(1)
    @transaction("morenfenzu")
    def task_morenfenzu(self):
        # 执行事务
        Transaction_Morenfenzu(self).run()

    def on_stop(self):
        super().on_stop()


class WebrunnerUser(CFastHttpUser):
    '''
    虚拟用户, 一个用户循环执行一个Action
    '''
    host = ""
    tasks = [WebrunnerAction]



    def __init__(self, *args, **kwargs) -> None:
        super().__init__(*args, **kwargs)

    def on_start(self):
        # 所有虚拟用户创建完成后开始执行，主要用于定义参数化和检查点的策略

        super().on_start()