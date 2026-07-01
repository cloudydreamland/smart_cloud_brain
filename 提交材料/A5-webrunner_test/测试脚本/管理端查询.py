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
        '用户数': 20,
        '创建速率': 10,
        '运行时长': 240
    }



class Transaction_Chaxun(SerialTransaction):
    '''
    事务定义, 一个事务由多个task构成, 每个task只包含一个请求
    '''

    def __init__(self, parent: "User") -> None:
        super().__init__(parent)

    @property
    def transaction(self):
        # 事务名称
        return "查询"

    def on_start(self):
        # 事务启动函数
        super().on_start()
    
    @task
    def task_0(self):
        url = "http://localhost:5175/api/admin/login"
        headers = {'Accept': '*/*', 'Accept-Language': 'zh-CN,zh;q=0.9', 'Connection': 'keep-alive', 'Content-Length': '39', 'Host': 'localhost:5175', 'Origin': 'http://localhost:5175', 'Referer': 'http://localhost:5175/login?redirect=/', 'Sec-Fetch-Dest': 'empty', 'Sec-Fetch-Mode': 'cors', 'Sec-Fetch-Site': 'same-origin', 'User-Agent': 'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/114.0.0.0 Safari/537.36', 'content-type': 'application/json', 'sec-ch-ua': '"Not.A/Brand";v="8", "Chromium";v="114"', 'sec-ch-ua-mobile': '?0', 'sec-ch-ua-platform': '"Windows"'}
        params = {}
        # application/json (dict)
        data = {}
        data['account'] = 'admin'
        data['password'] = '123456'

        res = self.post(url, headers=headers, params=params, json=data)
        # application/json,res.json()

    
    @task
    def task_1(self):
        url = "http://localhost:5175/api/admin/department/list"
        headers = {'Accept': '*/*', 'Accept-Language': 'zh-CN,zh;q=0.9', 'Connection': 'keep-alive', 'Host': 'localhost:5175', 'Referer': 'http://localhost:5175/', 'Sec-Fetch-Dest': 'empty', 'Sec-Fetch-Mode': 'cors', 'Sec-Fetch-Site': 'same-origin', 'User-Agent': 'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/114.0.0.0 Safari/537.36', 'authorization': 'Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiIxIiwicm9sZSI6IkFETUlOIiwibmFtZSI6Iuezu-e7n-euoeeQhuWRmCIsImV4cCI6MTc4Mjk1NDQ0M30.-Ix_t_822eqnlZAHw1UWaHiqLC9Gw1YCOXwEFxKtI_g', 'sec-ch-ua': '"Not.A/Brand";v="8", "Chromium";v="114"', 'sec-ch-ua-mobile': '?0', 'sec-ch-ua-platform': '"Windows"'}
        params = {}
        # null
        data = None
        res = self.get(url, headers=headers, params=params, data=data)
        # application/json,res.json()

    
    @task
    def task_2(self):
        url = "http://localhost:5175/api/admin/doctor/list"
        headers = {'Accept': '*/*', 'Accept-Language': 'zh-CN,zh;q=0.9', 'Connection': 'keep-alive', 'Host': 'localhost:5175', 'Referer': 'http://localhost:5175/', 'Sec-Fetch-Dest': 'empty', 'Sec-Fetch-Mode': 'cors', 'Sec-Fetch-Site': 'same-origin', 'User-Agent': 'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/114.0.0.0 Safari/537.36', 'authorization': 'Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiIxIiwicm9sZSI6IkFETUlOIiwibmFtZSI6Iuezu-e7n-euoeeQhuWRmCIsImV4cCI6MTc4Mjk1NDQ0M30.-Ix_t_822eqnlZAHw1UWaHiqLC9Gw1YCOXwEFxKtI_g', 'sec-ch-ua': '"Not.A/Brand";v="8", "Chromium";v="114"', 'sec-ch-ua-mobile': '?0', 'sec-ch-ua-platform': '"Windows"'}
        params = {}
        # null
        data = None
        res = self.get(url, headers=headers, params=params, data=data)
        # application/json,res.json()

    
    @task
    def task_3(self):
        url = "http://localhost:5175/api/admin/drug/list"
        headers = {'Accept': '*/*', 'Accept-Language': 'zh-CN,zh;q=0.9', 'Connection': 'keep-alive', 'Host': 'localhost:5175', 'Referer': 'http://localhost:5175/', 'Sec-Fetch-Dest': 'empty', 'Sec-Fetch-Mode': 'cors', 'Sec-Fetch-Site': 'same-origin', 'User-Agent': 'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/114.0.0.0 Safari/537.36', 'authorization': 'Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiIxIiwicm9sZSI6IkFETUlOIiwibmFtZSI6Iuezu-e7n-euoeeQhuWRmCIsImV4cCI6MTc4Mjk1NDQ0M30.-Ix_t_822eqnlZAHw1UWaHiqLC9Gw1YCOXwEFxKtI_g', 'sec-ch-ua': '"Not.A/Brand";v="8", "Chromium";v="114"', 'sec-ch-ua-mobile': '?0', 'sec-ch-ua-platform': '"Windows"'}
        params = {}
        # null
        data = None
        res = self.get(url, headers=headers, params=params, data=data)
        # application/json,res.json()

    
    @task
    def task_4(self):
        url = "http://localhost:5175/api/admin/ai-log/list"
        headers = {'Accept': '*/*', 'Accept-Language': 'zh-CN,zh;q=0.9', 'Connection': 'keep-alive', 'Host': 'localhost:5175', 'Referer': 'http://localhost:5175/', 'Sec-Fetch-Dest': 'empty', 'Sec-Fetch-Mode': 'cors', 'Sec-Fetch-Site': 'same-origin', 'User-Agent': 'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/114.0.0.0 Safari/537.36', 'authorization': 'Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiIxIiwicm9sZSI6IkFETUlOIiwibmFtZSI6Iuezu-e7n-euoeeQhuWRmCIsImV4cCI6MTc4Mjk1NDQ0M30.-Ix_t_822eqnlZAHw1UWaHiqLC9Gw1YCOXwEFxKtI_g', 'sec-ch-ua': '"Not.A/Brand";v="8", "Chromium";v="114"', 'sec-ch-ua-mobile': '?0', 'sec-ch-ua-platform': '"Windows"'}
        params = {}
        # null
        data = None
        res = self.get(url, headers=headers, params=params, data=data)
        # application/json,res.json()

    
    @task
    def task_5(self):
        url = "http://localhost:5175/api/admin/schedule/list"
        headers = {'Accept': '*/*', 'Accept-Language': 'zh-CN,zh;q=0.9', 'Connection': 'keep-alive', 'Host': 'localhost:5175', 'Referer': 'http://localhost:5175/', 'Sec-Fetch-Dest': 'empty', 'Sec-Fetch-Mode': 'cors', 'Sec-Fetch-Site': 'same-origin', 'User-Agent': 'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/114.0.0.0 Safari/537.36', 'authorization': 'Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiIxIiwicm9sZSI6IkFETUlOIiwibmFtZSI6Iuezu-e7n-euoeeQhuWRmCIsImV4cCI6MTc4Mjk1NDQ0M30.-Ix_t_822eqnlZAHw1UWaHiqLC9Gw1YCOXwEFxKtI_g', 'sec-ch-ua': '"Not.A/Brand";v="8", "Chromium";v="114"', 'sec-ch-ua-mobile': '?0', 'sec-ch-ua-platform': '"Windows"'}
        params = {}
        # null
        data = None
        res = self.get(url, headers=headers, params=params, data=data)
        # application/json,res.json()

    
    @task
    def task_6(self):
        url = "http://localhost:5175/api/admin/triage-desk/list"
        headers = {'Accept': '*/*', 'Accept-Language': 'zh-CN,zh;q=0.9', 'Connection': 'keep-alive', 'Host': 'localhost:5175', 'Referer': 'http://localhost:5175/', 'Sec-Fetch-Dest': 'empty', 'Sec-Fetch-Mode': 'cors', 'Sec-Fetch-Site': 'same-origin', 'User-Agent': 'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/114.0.0.0 Safari/537.36', 'authorization': 'Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiIxIiwicm9sZSI6IkFETUlOIiwibmFtZSI6Iuezu-e7n-euoeeQhuWRmCIsImV4cCI6MTc4Mjk1NDQ0M30.-Ix_t_822eqnlZAHw1UWaHiqLC9Gw1YCOXwEFxKtI_g', 'sec-ch-ua': '"Not.A/Brand";v="8", "Chromium";v="114"', 'sec-ch-ua-mobile': '?0', 'sec-ch-ua-platform': '"Windows"'}
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
    @transaction("查询")
    def task_chaxun(self):
        # 执行事务
        Transaction_Chaxun(self).run()

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