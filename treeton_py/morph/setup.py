# coding: utf-8

from setuptools import setup, find_packages

version = '0.1.1'
with open('README') as f:
    long_description = f.read()


setup(
    name='morph',
    version=version,
    description='Morphological paradigms for russioan based on starling morphological engine',
    long_description=long_description,
    author='Anatoli Starostin',
    author_email='anatoli.starostin@gmail.com',
    packages=find_packages(),
    include_package_data=True, install_requires=['attr', 'telegram']
)