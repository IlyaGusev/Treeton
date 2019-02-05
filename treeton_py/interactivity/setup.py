# coding: utf-8

from setuptools import setup, find_packages

version = '0.1.1'
with open('README') as f:
    long_description = f.read()


setup(
    name='interactivity',
    version=version,
    description='Server for interactive movie streaming',
    long_description=long_description,
    author='Anatoli Starostin',
    author_email='anatoli.starostin@gmail.com',
    packages=find_packages(),
    include_package_data=True, install_requires=[]
)