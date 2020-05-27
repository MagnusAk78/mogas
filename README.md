# Management of generic automation systems (Mogas)

## Table of contents
* [General info](#general-info)
* [Technologies](#technologies)
* [Setup](#setup)

## General info
Mogas is a web application that help with management of your automation system. It was created as a research project
with the purpose of exploring Automation ML (AML) and how it can be used to increase interoperability. The application
creates a database based on AML files and then allows users to add additional information (such as text and images)
connected to the AML entities. Changes that are made to the system, such as new nodes or changes in the hierarchy, this
can be updated by uploading a new AML file.

#### Research
The research results/ideas that came from this project can be found [here](https://doi.org/10.1007/978-3-319-73805-5_8). 

#### Current state
Unfortunately this is no longer working since it is based on an old version of MongoDB that is no longer supported.
It is possible that I will be able to update it in the future but there are currently no plans for it.
	
## Technologies
Project is created with:
* [Scala](https://www.scala-lang.org) 2.11.8 
* [Play Framework](https://www.playframework.com/) 2.5.8
* [Reactivemongo](http://reactivemongo.org/) 0.11.14
* [ScalaTest Plus](https://www.scalatest.org/plus) 1.5.1

## Setup
#### Install
* [sbt](https://www.scala-sbt.org)
