package com.sdd.marketplace.data.mappers

import com.sdd.marketplace.data.local.entities.*
import com.sdd.marketplace.data.remote.dto.*
import com.sdd.marketplace.domain.model.*

fun UserDto.toDomain() = User(
    id = id, fullName = fullName, email = email, phone = phone,
    avatarUrl = avatarUrl, bio = bio, isVerified = isVerified,
    isSeller = isSeller, rating = rating, reviewCount = reviewCount,
    followerCount = followerCount, followingCount = followingCount,
    productCount = productCount, soldCount = soldCount,
    responseRate = responseRate, location = location,
    joinedAt = joinedAt, isOnline = isOnline, lastSeen = lastSeen
)

fun ProductDto.toDomain() = Product(
    id = id, title = title, description = description, price = price,
    discountPrice = discountPrice, currency = currency, category = category,
    brand = brand, condition = condition, stockQuantity = stockQuantity,
    images = images, tags = tags, attributes = attributes,
    sellerId = sellerId, seller = seller?.toDomain(),
    location = location, latitude = latitude, longitude = longitude,
    deliveryOptions = deliveryOptions, returnPolicy = returnPolicy,
    isNegotiable = isNegotiable, isFeatured = isFeatured,
    isBoosted = isBoosted, isNew = isNew, isSold = isSold,
    viewCount = viewCount, favoriteCount = favoriteCount,
    rating = rating, reviewCount = reviewCount,
    createdAt = createdAt, updatedAt = updatedAt
)

fun MessageDto.toDomain() = Message(
    id = id, chatId = chatId, senderId = senderId,
    sender = sender?.toDomain(), content = content,
    type = MessageType.valueOf(type),
    imageUrl = imageUrl, latitude = latitude,
    longitude = longitude, locationAddress = locationAddress,
    isRead = isRead, isDelivered = isDelivered,
    sentAt = sentAt, editedAt = editedAt,
    replyToId = replyToMessageId,
    replyToContent = replyToContent,
    replyToSenderName = replyToSenderName
)

fun ChatDto.toDomain() = Chat(
    id = id, participants = participants.map { it.toDomain() },
    lastMessage = lastMessage?.toDomain(),
    unreadCount = unreadCount, createdAt = createdAt,
    updatedAt = updatedAt, productId = productId,
    product = product?.toDomain()
)

fun ReviewDto.toDomain() = Review(
    id = id, productId = productId, reviewerId = reviewerId,
    reviewer = reviewer?.toDomain(), sellerId = sellerId,
    rating = rating, comment = comment,
    isVerifiedPurchase = isVerifiedPurchase,
    createdAt = createdAt, helpfulCount = helpfulCount,
    isHelpfulByCurrentUser = isHelpfulByCurrentUser,
    replies = replies.map { r ->
        ReviewReply(id = r.id, reviewId = r.reviewId, authorId = r.authorId,
            author = r.author?.toDomain(), isSeller = r.isSeller, content = r.content, createdAt = r.createdAt)
    }
)

fun NotificationDto.toDomain() = Notification(
    id = id, userId = userId,
    type = runCatching { NotificationType.valueOf(type.uppercase()) }.getOrElse { NotificationType.SYSTEM },
    title = title, body = body, data = data,
    isRead = isRead, createdAt = createdAt
)

fun SupportTicketDto.toDomain() = SupportTicket(
    id = id, userId = userId,
    category = SupportCategory.entries.firstOrNull { it.name.lowercase() == category } ?: SupportCategory.OTHER,
    subject = subject, description = description,
    status = TicketStatus.entries.firstOrNull { it.name.lowercase() == status } ?: TicketStatus.OPEN,
    priority = TicketPriority.entries.firstOrNull { it.name.lowercase() == priority } ?: TicketPriority.MEDIUM,
    attachmentUrls = attachmentUrls, adminResponse = adminResponse, rating = rating,
    createdAt = createdAt, updatedAt = updatedAt
)

fun Product.toEntity() = ProductEntity(
    id = id, title = title, description = description,
    price = price, discountPrice = discountPrice,
    currency = currency, category = category, brand = brand,
    condition = condition, stockQuantity = stockQuantity,
    imagesJson = images.joinToString(","),
    tagsJson = tags.joinToString(","),
    sellerId = sellerId,
    sellerName = seller?.fullName ?: "",
    sellerAvatarUrl = seller?.avatarUrl,
    sellerIsVerified = seller?.isVerified ?: false,
    location = location, latitude = latitude, longitude = longitude,
    isNegotiable = isNegotiable, isFeatured = isFeatured,
    isBoosted = isBoosted, isNew = isNew, isSold = isSold,
    viewCount = viewCount, favoriteCount = favoriteCount,
    rating = rating, reviewCount = reviewCount,
    createdAt = createdAt, updatedAt = updatedAt
)

fun ProductEntity.toDomain() = Product(
    id = id, title = title, description = description,
    price = price, discountPrice = discountPrice,
    currency = currency, category = category, brand = brand,
    condition = condition, stockQuantity = stockQuantity,
    images = imagesJson.split(",").filter { it.isNotBlank() },
    tags = tagsJson.split(",").filter { it.isNotBlank() },
    attributes = emptyMap(), sellerId = sellerId,
    seller = User(id = sellerId, fullName = sellerName, email = null, phone = null,
        avatarUrl = sellerAvatarUrl, bio = null, isVerified = sellerIsVerified, isSeller = true,
        rating = 0.0, reviewCount = 0, followerCount = 0, followingCount = 0,
        productCount = 0, soldCount = 0, responseRate = 0, location = null,
        joinedAt = "", isOnline = false, lastSeen = null),
    location = location, latitude = latitude, longitude = longitude,
    deliveryOptions = emptyList(), returnPolicy = null,
    isNegotiable = isNegotiable, isFeatured = isFeatured,
    isBoosted = isBoosted, isNew = isNew, isSold = isSold,
    viewCount = viewCount, favoriteCount = favoriteCount,
    rating = rating, reviewCount = reviewCount,
    createdAt = createdAt, updatedAt = updatedAt
)

fun Message.toEntity() = MessageEntity(
    id = id, chatId = chatId, senderId = senderId,
    senderName = sender?.fullName ?: "", senderAvatarUrl = sender?.avatarUrl,
    content = content, type = type.name, imageUrl = imageUrl,
    latitude = latitude, longitude = longitude, locationAddress = locationAddress,
    isRead = isRead, isDelivered = isDelivered,
    isDeleted = isDeleted, isUnsent = isUnsent,
    sentAt = sentAt, editedAt = editedAt,
    replyToId = replyToId,
    replyToContent = replyToContent,
    replyToSenderName = replyToSenderName
)

fun MessageEntity.toDomain() = Message(
    id = id, chatId = chatId, senderId = senderId,
    sender = User(id = senderId, fullName = senderName, email = null, phone = null,
        avatarUrl = senderAvatarUrl, bio = null, isVerified = false, isSeller = false,
        rating = 0.0, reviewCount = 0, followerCount = 0, followingCount = 0,
        productCount = 0, soldCount = 0, responseRate = 0, location = null,
        joinedAt = "", isOnline = false, lastSeen = null),
    content = content, type = MessageType.valueOf(type),
    imageUrl = imageUrl, latitude = latitude, longitude = longitude,
    locationAddress = locationAddress, isRead = isRead, isDelivered = isDelivered,
    isDeleted = isDeleted, isUnsent = isUnsent,
    sentAt = sentAt, editedAt = editedAt,
    replyToId = replyToId,
    replyToContent = replyToContent,
    replyToSenderName = replyToSenderName
)

fun ChatEntity.toDomain() = Chat(
    id = id,
    participants = emptyList(),
    lastMessage = lastMessageContent?.let { content ->
        Message(
            id = "", chatId = id, senderId = "",
            sender = null, content = content,
            type = MessageType.valueOf(lastMessageType ?: "TEXT"),
            imageUrl = null, latitude = null, longitude = null,
            locationAddress = null, isRead = true, isDelivered = true,
            sentAt = lastMessageSentAt ?: "", editedAt = null
        )
    },
    unreadCount = unreadCount,
    createdAt = createdAt,
    updatedAt = updatedAt,
    productId = productId,
    product = productTitle?.let { title ->
        Product(
            id = productId ?: "", title = title, description = "", price = 0.0,
            discountPrice = null, currency = "UGX", category = "", brand = null,
            condition = "", stockQuantity = 0, images = listOfNotNull(productImageUrl),
            tags = emptyList(), attributes = emptyMap(), sellerId = "", seller = null,
            location = null, latitude = null, longitude = null, deliveryOptions = emptyList(),
            returnPolicy = null, isNegotiable = false, isFeatured = false, isBoosted = false,
            isNew = false, isSold = false, viewCount = 0, favoriteCount = 0, rating = 0.0,
            reviewCount = 0, createdAt = "", updatedAt = ""
        )
    }
)
